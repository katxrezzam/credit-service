package com.bootcamp.creditservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO de pago. Se aplica a la cuota impaga mas antigua; el monto debe coincidir exacto con esa
 * cuota (sin pagos parciales). La clave de idempotencia va por header, no en el body.
 *
 * <p>sourceAccountId es la cuenta (de account-service) de donde sale la plata - puede ser del
 * mismo cliente titular del credito o de un tercero (D8, Fase III: "un cliente puede hacer el
 * pago de cualquier producto de credito de terceros"). El pago debita esa cuenta de verdad, con
 * todas sus reglas (fondos, limite de movimientos, dia de plazo fijo).
 */
public record PaymentRequest(
        @NotNull(message = "amount es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount debe ser mayor a 0")
        BigDecimal amount,
        @NotBlank(message = "sourceAccountId es obligatorio")
        String sourceAccountId) {
}
