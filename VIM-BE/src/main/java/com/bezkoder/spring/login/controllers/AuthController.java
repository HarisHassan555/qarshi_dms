package com.bezkoder.spring.login.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.models.ERole;
import com.bezkoder.spring.login.models.Role;
import com.bezkoder.spring.login.models.User;
import com.bezkoder.spring.login.payload.request.LoginRequest;
import com.bezkoder.spring.login.payload.request.SignupRequest;
import com.bezkoder.spring.login.payload.response.JwtResponse;
import com.bezkoder.spring.login.payload.response.MessageResponse;
import com.bezkoder.spring.login.repository.RoleRepository;
import com.bezkoder.spring.login.repository.UserRepository;
import com.bezkoder.spring.login.sa.bll.dto.LoginResponse;
import com.bezkoder.spring.login.sa.bll.services.IAppActivityLogService;
import com.bezkoder.spring.login.security.jwt.JwtUtils;
import com.bezkoder.spring.login.security.services.UserDetailsImpl;
import com.bezkoder.spring.login.admin.utility.common.RequestMetadataUtil;

import com.bezkoder.spring.login.security.services.UserDetailsServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @Autowired
  private IAppActivityLogService activityLogService;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtUtils.generateJwtToken(authentication);
    
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    return ResponseEntity.ok(new JwtResponse(jwt,
                         userDetails.getId(), 
                         userDetails.getUsername(), 
                         userDetails.getEmail(), 
                         roles));
  }


  @PostMapping("/login")
  public LoginResponse login(@RequestBody com.bezkoder.spring.login.sa.bll.dto.LoginRequest loginRequest,
          HttpServletRequest request) {
    String ip = RequestMetadataUtil.resolveClientIp(request);
    String device = RequestMetadataUtil.resolveDevice(request);
    try {
      Authentication authentication = authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);

      String token = jwtUtils.generateJwtToken(authentication);
      UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
      String refreshToken = jwtUtils.generateRefreshToken(principal.getUsername());
      CfgTblUser user = userRepository.findByLoginNameForAuthentication(loginRequest.getUsername())
              .orElseThrow(() -> new RuntimeException("User not found for authenticated login"));
      LoginResponse loginResponse = new LoginResponse();
      loginResponse.setToken("Bearer " + token);
      loginResponse.setRefreshToken(refreshToken);
      loginResponse.setUser(user);

      Map<String, Object> payload = new HashMap<>();
      payload.put("username", loginRequest.getUsername());
      activityLogService.logActivity("LOGIN", user.getSerUserId(), user.getTxtUserName(), ip, device,
              "SUCCESS", "User logged in successfully", "USER", user.getSerUserId(), payload, null);
      return loginResponse;
    } catch (AuthenticationException ex) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("username", loginRequest.getUsername());
      activityLogService.logActivity("LOGIN", null, loginRequest.getUsername(), ip, device,
              "FAILURE", "Login failed", "USER", null, payload, ex.getMessage());
      throw ex;
    }
  }

  /**
   * Exchange a valid refresh token for a new access token (and rotated refresh token).
   * Clients call this when the access JWT expires so the user does not need to sign in again.
   */
  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
    String refreshToken = normalizeToken(body != null ? body.get("refreshToken") : null);
    if (refreshToken == null || refreshToken.isEmpty()) {
      Map<String, Object> error = new HashMap<>();
      error.put("status", "Failure");
      error.put("message", "refreshToken is required");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    if (!jwtUtils.validateRefreshToken(refreshToken)) {
      Map<String, Object> error = new HashMap<>();
      error.put("status", "Failure");
      error.put("message", "Refresh token is invalid or expired. Please sign in again.");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    try {
      String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
      UserDetails userDetails = userDetailsService.loadUserByUsername(username);
      String accessToken = jwtUtils.generateJwtTokenFromUsername(userDetails.getUsername());
      String nextRefreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());

      LoginResponse loginResponse = new LoginResponse();
      loginResponse.setToken("Bearer " + accessToken);
      loginResponse.setRefreshToken(nextRefreshToken);
      userRepository.findByLoginNameForAuthentication(username).ifPresent(loginResponse::setUser);
      return ResponseEntity.ok(loginResponse);
    } catch (Exception ex) {
      Map<String, Object> error = new HashMap<>();
      error.put("status", "Failure");
      error.put("message", ex.getMessage() != null ? ex.getMessage() : "Could not refresh token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
  }

  private static String normalizeToken(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
      return trimmed.substring(7).trim();
    }
    return trimmed;
  }

  /*@PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User(signUpRequest.getUsername(),
               signUpRequest.getEmail(),
               encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);

          break;
        case "mod":
          Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);

          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
  }*/
}
