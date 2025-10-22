package com.cash.mappers;

import com.cash.dtos.*;
import com.cash.grpc.userservice.*;

public class UserServiceDtoMapper {

  public static SignInRequest toProto(SignInRequestDto dto) {
    return SignInRequest.newBuilder()
        .setUsername(dto.getUsername())
        .setPassword(dto.getPassword())
        .build();
  }

  public static SignUpRequest toProto(SignUpRequestDto dto) {
    SignUpRequest.Builder builder = SignUpRequest.newBuilder()
        .setUsername(dto.getUsername())
        .setPassword(dto.getPassword())
        .setFirstName(dto.getFirstName())
        .setLastName(dto.getLastName())
        .setEmail(dto.getEmail());

    if (dto.getShippingAddress() != null) {
      builder.setShippingAddress(toProto(dto.getShippingAddress()));
    }

    return builder.build();
  }

  public static Address toProto(AddressDto dto) {
    return Address.newBuilder()
        .setStreetName(dto.getStreetName())
        .setStreetNumber(dto.getStreetNumber())
        .setCity(dto.getCity())
        .setCountry(dto.getCountry())
        .setPostalCode(dto.getPostalCode())
        .build();
  }

  public static ResetPasswordRequest toProto(ForgotPasswordRequestDto dto) {
    return ResetPasswordRequest.newBuilder()
        .setUsername(dto.getUsername())
        .setEmail(dto.getEmail())
        .build();
  }

  public static ConfirmPasswordResetRequest toProto(ResetPasswordRequestDto dto) {
    return ConfirmPasswordResetRequest.newBuilder()
        .setToken(dto.getToken())
        .setNewPassword(dto.getNewPassword())
        .build();
  }

  public static LogoutRequest toProto(LogoutRequestDto dto) {
    return LogoutRequest.newBuilder()
        .setJwt(dto.getJwt())
        .setUserId(dto.getUserId())
        .build();
  }
}
