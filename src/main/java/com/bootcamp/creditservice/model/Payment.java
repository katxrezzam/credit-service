package com.bootcamp.creditservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Historial auditable de pagos (el "movimiento" de un credito, D4: vive en credit-service). */
@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    private String creditId;

    private int installmentNumber;

    private BigDecimal amount;

    private Instant timestamp;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String correlationId;
}
