package com.bezkoder.spring.login.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.DispatcherType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value("${app.cors.allowed-origin-patterns:http://localhost:4200,http://127.0.0.1:4200}")
  private String allowedOriginPatterns;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    List<String> patterns = Arrays.stream(allowedOriginPatterns.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
    registry.addMapping("/**")
        .allowedOriginPatterns(patterns.toArray(new String[0]))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
        .allowedHeaders("*")
        .exposedHeaders("Authorization", "Content-Disposition")
        // Must be false here: many controllers use @CrossOrigin(origins="*"); credentials + "*" is invalid.
        .allowCredentials(false)
        .maxAge(3600);
  }

  @Bean
  public GlobalCorsFilter globalCorsFilter() {
    return new GlobalCorsFilter();
  }

  /** Runs before Spring Security (-100) so OPTIONS preflight always gets CORS headers. */
  @Bean
  public FilterRegistrationBean<GlobalCorsFilter> globalCorsFilterRegistration(GlobalCorsFilter filter) {
    FilterRegistrationBean<GlobalCorsFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
    return registration;
  }

  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilterServletRegistration(
      @Qualifier("corsConfigurationSource") CorsConfigurationSource source) {
    FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
    return registration;
  }

  @Bean
  @Primary
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> patterns = Arrays.stream(allowedOriginPatterns.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
    configuration.setAllowedOriginPatterns(patterns);
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
