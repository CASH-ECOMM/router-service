package com.cash.mappers;

import com.cash.dtos.PaymentRequestDTO;
import com.cash.dtos.PaymentResponseDTO;
import com.ecommerce.payment.grpc.*;
public final class PaymentServiceDtoMapper {

    private PaymentServiceDtoMapper() {}

    // Build a proto PaymentRequest from: UI DTO + aggregated data
    public static PaymentRequest toProto(PaymentRequestDTO dto,
                                         UserInfo userInfo,
                                         String itemId,
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

        return PaymentRequest.newBuilder()
                .setUserInfo(userInfo)
                .setItemId(itemId)
                .setItemCost(itemCostWholeDollars)              // int32, whole dollars
                .setShippingInfo(ship)
                .setCreditCardInfo(cc)
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