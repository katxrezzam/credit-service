package com.bootcamp.creditservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InstallmentResponse(
        int number,
        BigDecimal amount,
        LocalDate dueDate,
        boolean paid,
        Instant paidAt) {
}
