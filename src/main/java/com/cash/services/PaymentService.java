package com.cash.services;

import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.ecommerce.payment.grpc.*;

import io.grpc.Deadline;
import java.util.concurrent.TimeUnit;
@Service
public class PaymentService {

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub stub;

    // optional: centralize deadlines (avoid hanging)
    private PaymentServiceGrpc.PaymentServiceBlockingStub withDeadline() {
        return stub.withDeadline(Deadline.after(5, TimeUnit.SECONDS));
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        return withDeadline().processPayment(request);
    }

    public PaymentResponse getPaymentById(String paymentId) {
        GetPaymentRequest req = GetPaymentRequest.newBuilder()
                .setPaymentId(paymentId)
                .build();
        return withDeadline().getPaymentById(req);
    }

    public PaymentHistoryResponse getHistory(String userId, int page, int size) {
        PaymentHistoryRequest req = PaymentHistoryRequest.newBuilder()
                .setUserId(userId).setPage(page).setSize(size).build();
        return withDeadline().getPaymentHistory(req);
    }
}
