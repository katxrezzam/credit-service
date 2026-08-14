package com.bootcamp.creditservice.dto;

import com.bootcamp.creditservice.model.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCreditId(),
                payment.getInstallmentNumber(),
                payment.getAmount(),
                payment.getTimestamp());
    }
}
