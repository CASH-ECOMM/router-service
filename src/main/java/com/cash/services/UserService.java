package com.cash.services;

import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.cash.grpc.userservice.UserServiceGrpc;
import com.cash.grpc.userservice.*;

@Service
public class UserService {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public SignInResponse signIn(String username, String password) {
        SignInRequest request = SignInRequest.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build();
        return userServiceStub.signIn(request);
    }

    public SignUpResponse signUp(String username, String password, String firstName,
            String lastName, Address shippingAddress,
            String email) {
        SignUpRequest request = SignUpRequest.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setShippingAddress(shippingAddress)
                .setEmail(email)
                .build();
        return userServiceStub.signUp(request);
    }

    public ResetPasswordResponse forgotPassword(String username, String email) {
        ResetPasswordRequest request = ResetPasswordRequest.newBuilder()
                .setUsername(username)
                .setEmail(email)
                .build();
        return userServiceStub.resetPassword(request);
    }

    public ConfirmPasswordResetResponse resetPassword(String token, String newPassword) {
        ConfirmPasswordResetRequest request = ConfirmPasswordResetRequest.newBuilder()
                .setToken(token)
                .setNewPassword(newPassword)
                .build();
        return userServiceStub.confirmPasswordReset(request);
    }

    public ValidateTokenResponse validateToken(String jwt) {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setJwt(jwt)
                .build();
        return userServiceStub.validateToken(request);
    }

    public GetUserResponse getUser(int userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();
        return userServiceStub.getUser(request);
    }

    public LogoutResponse logout(String jwt, int userId) {
        LogoutRequest request = LogoutRequest.newBuilder()
                .setJwt(jwt)
                .setUserId(userId)
                .build();
        return userServiceStub.logout(request);
    }
}