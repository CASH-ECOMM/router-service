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

  public static AddressDto fromProto(Address address) {
    AddressDto dto = new AddressDto();
    dto.setStreetName(address.getStreetName());
    dto.setStreetNumber(address.getStreetNumber());
    dto.setCity(address.getCity());
    dto.setCountry(address.getCountry());
    dto.setPostalCode(address.getPostalCode());
    return dto;
  }

  public static GetUserResponseDto fromProto(GetUserResponse response) {
    return GetUserResponseDto.builder()
        .success(response.getSuccess())
        .userId(response.getUserId())
        .username(response.getUsername())
        .firstName(response.getFirstName())
        .lastName(response.getLastName())
        .shippingAddress(fromProto(response.getShippingAddress()))
        .email(response.getEmail())
        .message(response.getMessage())
        .build();
  }
}
