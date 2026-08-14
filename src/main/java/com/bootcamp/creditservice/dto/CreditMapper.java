package com.bootcamp.creditservice.dto;

import com.bootcamp.creditservice.model.Credit;
import com.bootcamp.creditservice.model.Installment;
import java.util.List;

/** Mapeo manual entidad Credit -&gt; DTO de salida. */
public final class CreditMapper {

    private CreditMapper() {
    }

    /** Convierte la entidad al DTO de salida, incluidas sus cuotas. */
    public static CreditResponse toResponse(Credit credit) {
        List<InstallmentResponse> installments = credit.getInstallments() == null
                ? List.of()
                : credit.getInstallments().stream().map(CreditMapper::toResponse).toList();
        return new CreditResponse(
                credit.getId(),
                credit.getCustomerId(),
                credit.getTotalAmount(),
                credit.getTermMonths(),
                credit.getInstallmentAmount(),
                credit.getStatus(),
                installments,
                credit.getCreatedAt(),
                credit.getUpdatedAt());
    }

    private static InstallmentResponse toResponse(Installment installment) {
        return new InstallmentResponse(
                installment.getNumber(),
                installment.getAmount(),
                installment.getDueDate(),
                installment.isPaid(),
                installment.getPaidAt());
    }
}
