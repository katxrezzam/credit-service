package com.bootcamp.creditservice.dto;

import com.bootcamp.creditservice.model.Payment;

/** Mapeo manual entidad Payment -&gt; DTO de salida. */
public final class PaymentMapper {

    private PaymentMapper() {
    }

    /** Convierte la entidad al DTO de salida. */
    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCreditId(),
                payment.getInstallmentNumber(),
                payment.getAmount(),
                payment.getTimestamp());
    }
}
