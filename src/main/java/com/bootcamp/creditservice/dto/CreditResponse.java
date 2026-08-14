package com.bootcamp.creditservice.dto;

import com.bootcamp.creditservice.model.CreditStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreditResponse(
        String id,
        String customerId,
        BigDecimal totalAmount,
        int termMonths,
        BigDecimal installmentAmount,
        CreditStatus status,
        List<InstallmentResponse> installments,
        Instant createdAt,
        Instant updatedAt) {
}
