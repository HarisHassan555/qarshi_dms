package com.bezkoder.spring.login.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared CORS response headers for servlet filters and security entry points.
 */
public final class CorsHeaders {

  private static final String ALLOWED_METHODS = "GET,POST,PUT,DELETE,PATCH,OPTIONS,HEAD";
  private static final String DEFAULT_ALLOWED_HEADERS =
      "Origin,Accept,X-Requested-With,Content-Type,Authorization,Cache-Control,Pragma";
  private static final String EXPOSED_HEADERS = "Authorization,x-auth-token,Content-Disposition";

  private static final Set<String> ALLOWED_ORIGINS = new HashSet<>(Arrays.asList(
      "http://localhost:4200",
      "http://127.0.0.1:4200"
  ));

  private CorsHeaders() {
  }

  public static void apply(HttpServletRequest request, HttpServletResponse response) {
    String origin = request.getHeader(HttpHeaders.ORIGIN);
    if (origin != null && isAllowedOrigin(origin)) {
      response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
      response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
      response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
    String requestHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    response.setHeader(
        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
        (requestHeaders != null && !requestHeaders.trim().isEmpty())
            ? requestHeaders
            : DEFAULT_ALLOWED_HEADERS);
    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_HEADERS);
    response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
  }

  public static boolean isPreflight(HttpServletRequest request) {
    return request != null && HttpMethod.OPTIONS.matches(request.getMethod());
  }

  private static boolean isAllowedOrigin(String origin) {
    String trimmed = origin.trim();
    if (ALLOWED_ORIGINS.contains(trimmed)) {
      return true;
    }
    return trimmed.startsWith("http://localhost:") || trimmed.startsWith("http://127.0.0.1:");
  }
}
