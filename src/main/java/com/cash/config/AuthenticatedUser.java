package com.cash.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Convenience helpers for reading authenticated user details from a request.
 */
public final class AuthenticatedUser {

  private AuthenticatedUser() {
  }

  public static Integer getUserId(HttpServletRequest request) {
    Object value = request.getAttribute(AuthenticationInterceptor.ATTR_USER_ID);
    return value instanceof Integer ? (Integer) value : null;
  }

  public static String getUsername(HttpServletRequest request) {
    Object value = request.getAttribute(AuthenticationInterceptor.ATTR_USERNAME);
    return value instanceof String ? (String) value : null;
  }

  public static String getRole(HttpServletRequest request) {
    Object value = request.getAttribute(AuthenticationInterceptor.ATTR_ROLE);
    return value instanceof String ? (String) value : null;
  }

  public static String getJwt(HttpServletRequest request) {
    Object value = request.getAttribute(AuthenticationInterceptor.ATTR_JWT);
    return value instanceof String ? (String) value : null;
  }
}
