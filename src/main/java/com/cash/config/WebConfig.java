package com.cash.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link AuthenticationInterceptor} and defines which routes
 * remain public.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final AuthenticationInterceptor authenticationInterceptor;

  public WebConfig(AuthenticationInterceptor authenticationInterceptor) {
    this.authenticationInterceptor = authenticationInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authenticationInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/users/signin",
            "/api/users/signup",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/users/validate-token",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**");
  }
}
