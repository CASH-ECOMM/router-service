package com.cash.config;

import org.springframework.context.annotation.Configuration;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfiguration {
    @Value("${grpc.client.payment-service.host:localhost}")
    private String paymentServiceHost;

    @Value("${grpc.client.payment-service.port:9090}")
    private int paymentServicePort;

    @Bean(name = "paymentServiceChannel")
    public ManagedChannel paymentServiceChannel() {
        return ManagedChannelBuilder
                .forAddress(paymentServiceHost, paymentServicePort)
                .usePlaintext()
                .build();
    }
}

