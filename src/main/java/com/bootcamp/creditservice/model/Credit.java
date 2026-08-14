package com.bootcamp.creditservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Producto de credito. Persistida en la coleccion "credits" de creditdb. No requiere que el
 * cliente tenga una cuenta bancaria (a diferencia de account-service, el enunciado lo dice
 * explicito: "un cliente puede tener un producto de credito sin la obligacion de tener una
 * cuenta bancaria en la institucion").
 */
@Document(collection = "credits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credit {

    @Id
    private String id;

    private String customerId;

    private BigDecimal totalAmount;

    private int termMonths;

    private BigDecimal installmentAmount;

    private CreditStatus status;

    private List<Installment> installments;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
