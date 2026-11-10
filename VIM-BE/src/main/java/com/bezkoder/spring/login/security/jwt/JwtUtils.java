package com.bezkoder.spring.login.security.jwt;

import java.security.Key;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.bezkoder.spring.login.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  private static final String CLAIM_TOKEN_TYPE = "tokenType";
  private static final String TOKEN_TYPE_ACCESS = "access";
  private static final String TOKEN_TYPE_REFRESH = "refresh";

  @Value("${bezkoder.app.jwtSecret}")
  private String jwtSecret;

  @Value("${bezkoder.app.jwtExpirationMs}")
  private int jwtExpirationMs;

  @Value("${bezkoder.app.jwtRefreshExpirationMs:604800000}")
  private long jwtRefreshExpirationMs;

  public String generateJwtToken(Authentication authentication) {
    UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
    return generateJwtTokenFromUsername(userPrincipal.getUsername());
  }

  public String generateJwtTokenFromUsername(String username) {
    return Jwts.builder()
        .setSubject(username)
        .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
        .setIssuedAt(new Date())
        .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String generateRefreshToken(String username) {
    return Jwts.builder()
        .setSubject(username)
        .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
        .setIssuedAt(new Date())
        .setExpiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }
  
  private Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }

  public String getUserNameFromJwtToken(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      Claims claims = parseClaims(authToken);
      // Refresh tokens must not authorize normal API calls.
      if (isRefreshTokenType(claims)) {
        logger.warn("Refresh token rejected as access token");
        return false;
      }
      return true;
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }

  public boolean validateRefreshToken(String refreshToken) {
    try {
      Claims claims = parseClaims(refreshToken);
      return isRefreshTokenType(claims);
    } catch (MalformedJwtException e) {
      logger.error("Invalid refresh token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("Refresh token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("Refresh token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("Refresh token claims string is empty: {}", e.getMessage());
    }
    return false;
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(key()).build()
        .parseClaimsJws(token).getBody();
  }

  private boolean isRefreshTokenType(Claims claims) {
    if (claims == null) {
      return false;
    }
    Object type = claims.get(CLAIM_TOKEN_TYPE);
    return type != null && TOKEN_TYPE_REFRESH.equalsIgnoreCase(String.valueOf(type));
  }
}
