package com.bootcamp.creditservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** DTO de salida de un pago de credito. */
public record PaymentResponse(
        String id,
        String creditId,
        int installmentNumber,
        BigDecimal amount,
        Instant timestamp) {
}
