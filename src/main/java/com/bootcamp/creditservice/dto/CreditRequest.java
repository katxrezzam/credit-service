package com.bootcamp.creditservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO de creacion. Las cuotas NO se piden aca: se generan automaticamente (monto total dividido
 * en termMonths cuotas iguales, con la diferencia de redondeo absorbida en la ultima cuota para
 * que la suma cierre exacta contra totalAmount).
 */
public record CreditRequest(
        @NotBlank(message = "customerId es obligatorio") String customerId,
        @NotNull(message = "totalAmount es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "totalAmount debe ser mayor a 0")
        BigDecimal totalAmount,
        @NotNull(message = "termMonths es obligatorio")
        @Min(value = 1, message = "termMonths debe ser al menos 1")
        @Max(value = 360, message = "termMonths no puede superar 360 (30 anios)")
        Integer termMonths) {
}
