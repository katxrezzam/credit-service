package com.bootcamp.creditservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuota de un credito. Embebida dentro de {@link Credit} (no es su propia coleccion): el
 * cronograma de cuotas es fijo y acotado (termMonths), a diferencia de los movimientos de
 * account-service que crecen sin limite - no justifica una coleccion aparte con referencias.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

    private int number;

    private BigDecimal amount;

    private LocalDate dueDate;

    private boolean paid;

    private Instant paidAt;
}
