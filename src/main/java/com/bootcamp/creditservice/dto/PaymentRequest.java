package com.bootcamp.creditservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO de pago. Se aplica a la cuota impaga mas antigua; el monto debe coincidir exacto con esa
 * cuota (sin pagos parciales). La clave de idempotencia va por header, no en el body.
 */
public record PaymentRequest(
        @NotNull(message = "amount es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount debe ser mayor a 0")
        BigDecimal amount) {
}
