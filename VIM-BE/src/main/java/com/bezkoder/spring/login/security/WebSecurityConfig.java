package com.bezkoder.spring.login.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.Enable                                                  ;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.bezkoder.spring.login.security.jwt.AuthEntryPointJwt;
import com.bezkoder.spring.login.security.jwt.AuthTokenFilter;
import com.bezkoder.spring.login.security.services.UserDetailsServiceImpl;

@Configuration
@EnableMethodSecurity
// (securedEnabled = true,
// jsr250Enabled = true,
// prePostEnabled = true) // by default
public class WebSecurityConfig { // extends WebSecurityConfigurerAdapter {
  @Autowired
  UserDetailsServiceImpl userDetailsService;

  @Autowired
  private AuthEntryPointJwt unauthorizedHandler;

  @Autowired
  @Qualifier("corsConfigurationSource")
  private CorsConfigurationSource corsConfigurationSource;

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }

//  @Override
//  public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
//    authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
//  }
  
  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
      DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
       
      authProvider.setUserDetailsService(userDetailsService);
      authProvider.setPasswordEncoder(passwordEncoder());
   
      return authProvider;
  }

//  @Bean
//  @Override
//  public AuthenticationManager authenticationManagerBean() throws Exception {
//    return super.authenticationManagerBean();
//  }
  
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

//  @Override
//  protected void configure(HttpSecurity http) throws Exception {
//    http.cors().and().csrf().disable()
//      .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
//      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
//      .authorizeRequests().antMatchers("/api/auth/**").permitAll()
//      .antMatchers("/api/test/**").permitAll()
//      .anyRequest().authenticated();
//
//    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
//  }
  
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.disable())
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
          auth.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
              .antMatchers("/api/auth/**").permitAll()
              .antMatchers("/api/test/**").permitAll()
                  .antMatchers("/", "/index.html", "/static/**", "/assets/**",
                      "/*.js", "/*.mjs", "/*.css", "/*.map", "/*.json",
                      "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.webp", "/*.ico",
                      "/*.woff", "/*.woff2", "/*.ttf", "/*.eot",
                      "/**/*.js", "/**/*.mjs", "/**/*.css", "/**/*.map",
                      "/**/*.png", "/**/*.jpg", "/**/*.jpeg", "/**/*.gif", "/**/*.svg", "/**/*.webp", "/**/*.ico",
                      "/**/*.woff", "/**/*.woff2", "/**/*.ttf", "/**/*.eot").permitAll()
                  .antMatchers("/{path:[^\\.]*}", "/**/{path:[^\\.]*}").permitAll()
                  .antMatchers("/login", "/getloginCustomer", "/allMenu", "/resources/**", "/updatePasswordReconfirm").permitAll()
                  .antMatchers("/getSignature", "/getUserSignaturePath").permitAll()
              .antMatchers("/ws/chat", "/ws/chat/**", "/ws/notifications", "/ws/notifications/**").permitAll()
                  .antMatchers("/approveApplicationFromEmail", "/rejectApplicationFromEmail", "/sendBackApplicationFromEmail", "/sendBackToInitiatorFromEmail",
                          "/holdTemplateApplicationFromEmail", "/unholdTemplateApplicationFromEmail",
                          "/opinionApplicationFromEmail").permitAll()
                  .antMatchers("/public/documentShare", "/public/documentSharePdf").permitAll()
                  .anyRequest().authenticated()
        );
    
    http.authenticationProvider(authenticationProvider());

      http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

      return http.build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return objectMapper;
  }

}
