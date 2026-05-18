package com.bezkoder.spring.login.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalCorsFilter extends OncePerRequestFilter {

    private static final String ALLOWED_METHODS = "GET,POST,PUT,DELETE,PATCH,OPTIONS";
    private static final String DEFAULT_ALLOWED_HEADERS =
            "Origin,Accept,X-Requested-With,Content-Type,Authorization,Cache-Control,Pragma";
    private static final String EXPOSED_HEADERS = "Authorization,x-auth-token,Content-Disposition";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.trim().isEmpty()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
        String requestHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                (requestHeaders != null && !requestHeaders.trim().isEmpty()) ? requestHeaders : DEFAULT_ALLOWED_HEADERS);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_HEADERS);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}

