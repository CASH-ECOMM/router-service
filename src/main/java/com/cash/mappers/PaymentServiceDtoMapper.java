package com.cash.mappers;

import com.cash.dtos.PaymentRequestDTO;
import com.cash.dtos.PaymentResponseDTO;
import com.cash.dtos.TotalCostDTO;
import com.cash.grpc.userservice.Address;
import com.cash.grpc.userservice.GetUserResponse;
import com.ecommerce.payment.grpc.*;
public final class PaymentServiceDtoMapper {

    private PaymentServiceDtoMapper() {}

    private static final String DEFAULT_PROVINCE = "Ontario";

    public static UserInfo toProtoUser(GetUserResponse user) { // CHANGED
        if (!user.getSuccess()) {
            throw new IllegalArgumentException("User lookup failed: " + user.getMessage());
        }
        Address a = user.getShippingAddress();
        if (a == null) {
            throw new IllegalArgumentException("User has no shipping address");
        }
        return UserInfo.newBuilder()
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setStreet(a.getStreetName())
                .setNumber(a.getStreetNumber())       // proto expects STRING
                .setProvince(DEFAULT_PROVINCE)        // use global default
                .setCountry(a.getCountry())
                .setPostalCode(a.getPostalCode())
                .setUserId(user.getUserId())
                .build();
    }
    // Build a proto PaymentRequest from: UI DTO + aggregated data
    public static PaymentRequest toProto(PaymentRequestDTO dto,
                                         GetUserResponse user,
                                         int itemId,
                                         int itemCostWholeDollars,
                                         int shippingCostWholeDollars,
                                         int estimatedDays) {

        CreditCardInfo cc = CreditCardInfo.newBuilder()
                .setCardNumber(dto.getCreditCard().getCardNumber())
                .setNameOnCard(dto.getCreditCard().getNameOnCard())
                .setExpiryDate(dto.getCreditCard().getExpiryDate())
                .setSecurityCode(dto.getCreditCard().getSecurityCode())
                .build();

        ShippingInfo ship = ShippingInfo.newBuilder()
                .setShippingType(dto.getShippingType() == PaymentRequestDTO.ShippingTypeDTO.EXPEDITED
                        ? ShippingType.EXPEDITED : ShippingType.REGULAR)
                .setShippingCost(shippingCostWholeDollars)     // int32, whole dollars
                .setEstimatedDays(estimatedDays)
                .build();
        UserInfo userInfo = toProtoUser(user);
        return PaymentRequest.newBuilder()
                .setUserInfo(userInfo)
                .setItemId(itemId)
                .setItemCost(itemCostWholeDollars)              // int32, whole dollars
                .setShippingInfo(ship)
                .setCreditCardInfo(cc)
                .build();
    }
    public static PaymentRequest toProtoQuote(
            int itemId,
            int itemCostWholeDollars,
            int shippingCostWholeDollars,
            int estimatedDays,
            PaymentRequestDTO.ShippingTypeDTO type
    ) {
        ShippingInfo ship = ShippingInfo.newBuilder()
                .setShippingType(type == PaymentRequestDTO.ShippingTypeDTO.EXPEDITED
                        ? ShippingType.EXPEDITED : ShippingType.REGULAR)
                .setShippingCost(shippingCostWholeDollars)
                .setEstimatedDays(estimatedDays)
                .build();

        return PaymentRequest.newBuilder()
                .setItemId(itemId)
                .setItemCost(itemCostWholeDollars)
                .setShippingInfo(ship)
                .build();
    }
    // proto → TotalCostDTO
    public static TotalCostDTO fromProto(TotalCostResponse t) {
        return TotalCostDTO.builder()
                .itemCost(t.getItemCost())
                .hstRate(t.getHstRate())
                .hstAmount(t.getHstAmount())
                .totalCost(t.getTotalCost())
                .message(t.getMessage())
                .build();
    }

    // Convert proto → REST DTO for UI
    public static PaymentResponseDTO fromProto(PaymentResponse r) {
        PaymentResponseDTO.PaymentResponseDTOBuilder b = PaymentResponseDTO.builder()
                .success(r.getSuccess())
                .paymentId(r.getPaymentId())
                .message(r.getMessage())
                .transactionDate(r.getTransactionDate())
                .shippingMessage(r.getShippingMessage());

        if (r.hasReceiptInfo()) {
            ReceiptInfo ri = r.getReceiptInfo();
            // item_cost & shipping_cost are int32 (whole dollars) in proto;
            b.receipt(PaymentResponseDTO.ReceiptDTO.builder()
                    .receiptId(ri.getReceiptId())
                    .firstName(ri.getFirstName())
                    .lastName(ri.getLastName())
                    .address(ri.getFullAddress())
                    .itemCost(ri.getItemCost())
                    .shippingCost(ri.getShippingCost())
                    .hstAmount(ri.getHstAmount())
                    .totalPaid(ri.getTotalPaid())
                    .itemId(ri.getItemId())
                    .build());
        }
        return b.build();
    }
}