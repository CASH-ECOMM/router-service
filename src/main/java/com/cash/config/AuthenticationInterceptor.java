package com.cash.config;

import com.cash.grpc.userservice.ValidateTokenResponse;
import com.cash.services.UserService;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Validates JWT tokens on protected routes and exposes user details as request
 * attributes.
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

  public static final String ATTR_USER_ID = "authenticatedUserId";
  public static final String ATTR_USERNAME = "authenticatedUsername";
  public static final String ATTR_ROLE = "authenticatedRole";
  public static final String ATTR_JWT = "authenticatedJwt";

  private final UserService userService;

  public AuthenticationInterceptor(UserService userService) {
    this.userService = userService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeUnauthorized(response, "Missing or invalid Authorization header");
      return false;
    }

    String jwt = authHeader.substring(7).trim();
    if (jwt.isEmpty()) {
      writeUnauthorized(response, "Authorization token cannot be empty");
      return false;
    }

    try {
      ValidateTokenResponse validation = userService.validateToken(jwt);
      if (!validation.getValid()) {
        writeUnauthorized(response, "Token validation failed");
        return false;
      }

      request.setAttribute(ATTR_USER_ID, validation.getUserId());
      request.setAttribute(ATTR_USERNAME, validation.getUsername());
      request.setAttribute(ATTR_ROLE, validation.getRole());
      request.setAttribute(ATTR_JWT, jwt);
      return true;
    } catch (StatusRuntimeException ex) {
      writeUnauthorized(response, ex.getMessage());
      return false;
    }
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\": \"" + message + "\"}");
  }
}
