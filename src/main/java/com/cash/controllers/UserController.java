package com.cash.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import com.cash.config.AuthenticatedUser;
import com.cash.config.BiddingSessionManager;
import com.cash.dtos.*;
import com.cash.dtos.ValidateTokenResponseDto;
import com.cash.exceptions.UnauthorizedException;
import com.cash.grpc.userservice.*;
import com.cash.mappers.UserServiceDtoMapper;
import com.cash.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User authentication and account management")
public class UserController {
    private final UserService userService;
    private final BiddingSessionManager biddingSessionManager;

    @Autowired
    public UserController(UserService userService, BiddingSessionManager biddingSessionManager) {
        this.userService = userService;
        this.biddingSessionManager = biddingSessionManager;
    }

    @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = SignInResponseDto.class)))
    @PostMapping("/signin")
    public ResponseEntity<SignInResponseDto> signIn(
            @Parameter(description = "Sign-in credentials", required = true) @Valid @RequestBody SignInRequestDto dto,
            HttpSession session) {
        SignInRequest request = UserServiceDtoMapper.toProto(dto);
        SignInResponse response = userService.signIn(request.getUsername(), request.getPassword());

        if (!response.getSuccess()) {
            throw new UnauthorizedException(response.getMessage());
        }

        biddingSessionManager.clear(session);

        SignInResponseDto responseDto = SignInResponseDto.builder()
                .jwt(response.getJwt())
                .userId(response.getUserId())
                .message(response.getMessage())
                .build();

        // Add HATEOAS links
        responseDto.add(
                linkTo(methodOn(UserController.class).getUser(response.getUserId(), null))
                        .withRel("profile"));
        responseDto.add(
                linkTo(methodOn(UserController.class).logout(null, null, session)).withRel("logout"));
        responseDto.add(
                linkTo(methodOn(CatalogueController.class).getAllItems()).withRel("catalogue"));

        return ResponseEntity.ok(responseDto);
    }

    @ApiResponse(responseCode = "201", description = "User successfully created", content = @Content(schema = @Schema(implementation = SignUpResponseDto.class)))
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDto> signUp(
            @Parameter(description = "User registration details", required = true) @Valid @RequestBody SignUpRequestDto dto) {
        SignUpRequest request = UserServiceDtoMapper.toProto(dto);
        SignUpResponse response = userService.signUp(
                request.getUsername(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getShippingAddress(),
                request.getEmail());

        if (!response.getSuccess()) {
            throw new IllegalArgumentException(response.getMessage());
        }

        SignUpResponseDto responseDto = SignUpResponseDto.builder()
                .userId(response.getUserId())
                .message(response.getMessage())
                .build();

        // Add HATEOAS links
        responseDto.add(linkTo(methodOn(UserController.class).signIn(null, null)).withRel("signin"));
        responseDto.add(
                linkTo(methodOn(UserController.class).getUser(response.getUserId(), null))
                        .withRel("profile"));

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully", content = @Content(schema = @Schema(implementation = GetUserResponseDto.class)))
    @GetMapping("/{userId}")
    public ResponseEntity<GetUserResponseDto> getUser(
            @Parameter(description = "User ID", required = true) @PathVariable int userId,
            HttpServletRequest request) {
        Integer authenticatedUserId = AuthenticatedUser.getUserId(request);
        if (authenticatedUserId == null || authenticatedUserId != userId) {
            throw new UnauthorizedException("You are not authorized to view this profile");
        }
        GetUserResponse response = userService.getUser(userId);

        if (!response.getSuccess()) {
            throw new com.cash.exceptions.ResourceNotFoundException(response.getMessage());
        }

        GetUserResponseDto dto = UserServiceDtoMapper.fromProto(response);

        // Add HATEOAS links
        dto.add(linkTo(methodOn(UserController.class).getUser(userId, request)).withSelfRel());
        dto.add(linkTo(methodOn(UserController.class).logout(null, request, null)).withRel("logout"));
        dto.add(linkTo(methodOn(CatalogueController.class).getAllItems()).withRel("catalogue"));

        return ResponseEntity.ok(dto);
    }

    @ApiResponse(responseCode = "200", description = "Token validation result", content = @Content(schema = @Schema(implementation = ValidateTokenResponseDto.class)))
    @PostMapping("/validate-token")
    public ResponseEntity<ValidateTokenResponseDto> validateToken(
            @Parameter(description = "JWT token in format: Bearer <token>", required = false) @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorization header is required");
        }

        String jwt = token.replace("Bearer ", "").trim();

        if (jwt.isEmpty()) {
            throw new IllegalArgumentException("JWT token is required");
        }

        ValidateTokenResponse response = userService.validateToken(jwt);

        ValidateTokenResponseDto dto = ValidateTokenResponseDto.builder()
                .valid(response.getValid())
                .userId(response.getUserId())
                .username(response.getUsername())
                .role(response.getRole())
                .message(response.getMessage())
                .build();

        // Add HATEOAS links
        dto.add(linkTo(methodOn(UserController.class).validateToken(token)).withSelfRel());
        if (response.getValid()) {
            dto.add(
                    linkTo(methodOn(UserController.class).getUser(response.getUserId(), null))
                            .withRel("profile"));
        }

        return ResponseEntity.ok(dto);
    }

    @ApiResponse(responseCode = "200", description = "Password reset email sent", content = @Content(schema = @Schema(implementation = PasswordResetResponseDto.class)))
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponseDto> forgotPassword(
            @Parameter(description = "Password reset request with username and email", required = true) @Valid @RequestBody ForgotPasswordRequestDto dto) {
        ResetPasswordRequest request = UserServiceDtoMapper.toProto(dto);
        ResetPasswordResponse response = userService.forgotPassword(request.getUsername(), request.getEmail());

        if (!response.getSuccess()) {
            throw new IllegalArgumentException(response.getMessage());
        }

        PasswordResetResponseDto responseDto = PasswordResetResponseDto.builder().message(response.getMessage())
                .build();

        // Add HATEOAS links
        responseDto.add(
                linkTo(methodOn(UserController.class).resetPassword(null)).withRel("reset-password"));
        responseDto.add(linkTo(methodOn(UserController.class).signIn(null, null)).withRel("signin"));

        return ResponseEntity.ok(responseDto);
    }

    @ApiResponse(responseCode = "200", description = "Password successfully reset", content = @Content(schema = @Schema(implementation = PasswordResetResponseDto.class)))
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponseDto> resetPassword(
            @Parameter(description = "Reset token and new password", required = true) @Valid @RequestBody ResetPasswordRequestDto dto) {
        ConfirmPasswordResetRequest request = UserServiceDtoMapper.toProto(dto);
        ConfirmPasswordResetResponse response = userService.resetPassword(request.getToken(), request.getNewPassword());

        if (!response.getSuccess()) {
            throw new IllegalArgumentException(response.getMessage());
        }

        PasswordResetResponseDto responseDto = PasswordResetResponseDto.builder().message(response.getMessage())
                .build();

        // Add HATEOAS links
        responseDto.add(linkTo(methodOn(UserController.class).signIn(null, null)).withRel("signin"));

        return ResponseEntity.ok(responseDto);
    }

    @ApiResponse(responseCode = "200", description = "Successfully logged out", content = @Content(schema = @Schema(implementation = LogoutResponseDto.class)))
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDto> logout(
            @Parameter(description = "Logout request with JWT and user ID", required = true) @Valid @RequestBody LogoutRequestDto dto,
            HttpServletRequest request,
            HttpSession session) {
        Integer authenticatedUserId = AuthenticatedUser.getUserId(request);
        if (authenticatedUserId == null || dto.getUserId() != authenticatedUserId) {
            throw new UnauthorizedException("You are not authorized to perform this action");
        }
        LogoutRequest logoutRequest = UserServiceDtoMapper.toProto(dto);
        LogoutResponse response = userService.logout(logoutRequest.getJwt(), authenticatedUserId);
        biddingSessionManager.clear(session);

        LogoutResponseDto responseDto = LogoutResponseDto.builder().message(response.getMessage()).build();

        // Add HATEOAS links
        responseDto.add(linkTo(methodOn(UserController.class).signIn(null, null)).withRel("signin"));
        responseDto.add(linkTo(methodOn(CatalogueController.class).getAllItems()).withRel("catalogue"));

        return ResponseEntity.ok(responseDto);
    }
}
