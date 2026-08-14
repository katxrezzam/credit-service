package com.bootcamp.creditservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO de actualizacion: solo permite corregir totalAmount/termMonths, y solo si el credito
 * todavia no tiene ninguna cuota pagada (se rechaza en el service si ya se pago algo - no tiene
 * sentido reescribir un cronograma con historial de pagos real). customerId no es editable (no
 * tiene sentido de negocio "transferir" un credito a otro cliente).
 */
public record CreditUpdateRequest(
        @NotNull(message = "totalAmount es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "totalAmount debe ser mayor a 0")
        BigDecimal totalAmount,
        @NotNull(message = "termMonths es obligatorio")
        @Min(value = 1, message = "termMonths debe ser al menos 1")
        @Max(value = 360, message = "termMonths no puede superar 360 (30 anios)")
        Integer termMonths) {
}
