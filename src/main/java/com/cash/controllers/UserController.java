package com.cash.controllers;

import com.cash.dtos.*;
import com.cash.services.UserService;
import com.cash.mappers.UserServiceDtoMapper;
import com.cash.grpc.userservice.*;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody SignInRequestDto dto) {
        try {
            SignInRequest request = UserServiceDtoMapper.toProto(dto);
            SignInResponse response = userService.signIn(
                    request.getUsername(),
                    request.getPassword());

            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "jwt", response.getJwt(),
                        "userId", response.getUserId(),
                        "message", response.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequestDto dto) {
        try {
            SignUpRequest request = UserServiceDtoMapper.toProto(dto);
            SignUpResponse response = userService.signUp(
                    request.getUsername(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getShippingAddress(),
                    request.getEmail());

            if (response.getSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "userId", response.getUserId(),
                                "message", response.getMessage()));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        try {
            GetUserResponse response = userService.getUser(userId);

            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.replace("Bearer ", "");
            ValidateTokenResponse response = userService.validateToken(jwt);

            return ResponseEntity.ok(Map.of(
                    "valid", response.getValid(),
                    "userId", response.getUserId(),
                    "username", response.getUsername(),
                    "role", response.getRole()));
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDto dto) {
        try {
            ResetPasswordRequest request = UserServiceDtoMapper.toProto(dto);
            ResetPasswordResponse response = userService.forgotPassword(
                    request.getUsername(),
                    request.getEmail());

            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of("message", response.getMessage()));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto dto) {
        try {
            ConfirmPasswordResetRequest request = UserServiceDtoMapper.toProto(dto);
            ConfirmPasswordResetResponse response = userService.resetPassword(
                    request.getToken(),
                    request.getNewPassword());

            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of("message", response.getMessage()));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequestDto dto) {
        try {
            LogoutRequest request = UserServiceDtoMapper.toProto(dto);
            LogoutResponse response = userService.logout(
                    request.getJwt(),
                    request.getUserId());

            return ResponseEntity.ok(Map.of("message", response.getMessage()));
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}