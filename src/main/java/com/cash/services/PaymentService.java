package com.cash.services;

import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.ecommerce.payment.grpc.*;

import io.grpc.Deadline;
import java.util.concurrent.TimeUnit;
@Service
public class PaymentService {

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub stub;

    private PaymentServiceGrpc.PaymentServiceBlockingStub withDeadline() {
        return stub.withDeadline(Deadline.after(5, TimeUnit.SECONDS));
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        return withDeadline().processPayment(request);
    }

    public PaymentResponse getPaymentById(int paymentId) {
        GetPaymentRequest req = GetPaymentRequest.newBuilder()
                .setPaymentId(paymentId)
                .build();
        return withDeadline().getPaymentById(req);
    }

    public PaymentHistoryResponse getHistory(int userId, int page, int size) {
        PaymentHistoryRequest req = PaymentHistoryRequest.newBuilder()
                .setUserId(userId).setPage(page).setSize(size).build();
        return withDeadline().getPaymentHistory(req);
    }
    public TotalCostResponse calculateTotalCost(PaymentRequest request) throws StatusRuntimeException {
        return withDeadline().calculateTotalCost(request);
    }

    public TotalCostResponse calculateTotalCost(
            Integer itemId,
            Integer itemCost,
            String shippingType,   // "REGULAR" | "EXPEDITED" (case-insensitive)
            Integer shippingCost,
            Integer estimatedDays
    ){
        ShippingType type = parseShippingType(shippingType);
        ShippingInfo ship = ShippingInfo.newBuilder()
                .setShippingType(type)
                .setShippingCost(nz(shippingCost))
                .setEstimatedDays(nz(estimatedDays))
                .build();

        PaymentRequest req = PaymentRequest.newBuilder()
                .setItemId(nz(itemId))
                .setItemCost(nz(itemCost))
                .setShippingInfo(ship)
                .build();

        return calculateTotalCost(req);
    }


    private static int nz(Integer v) { return v == null ? 0 : v; }

    private static ShippingType parseShippingType(String v) {
        if (v == null) return ShippingType.REGULAR;
        switch (v.trim().toUpperCase()) {
            case "EXPEDITED": return ShippingType.EXPEDITED;
            default:          return ShippingType.REGULAR;
        }
    }
}


