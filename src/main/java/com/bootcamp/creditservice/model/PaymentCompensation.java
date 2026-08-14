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

/**
 * Marca que un intento de pago debito la cuenta origen pero no pudo aplicarse localmente, y ya
 * se compenso (se revirtio el debito) - D6/D7 cross-servicio. Existe solo para poder rechazar de
 * forma explicita un reintento con la MISMA Idempotency-Key: sin este registro, el reintento
 * encontraria el retiro ya cacheado en account-service (idempotente por clave) y no volveria a
 * debitar, pero SI aplicaria el pago localmente - un pago "gratis", sin plata detras. Mismo
 * espiritu que el "reintentar con clave nueva" ya documentado para transferencias, resuelto aqui
 * con un registro local en vez de una consulta remota a account-service.
 */
@Document(collection = "payment_compensations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompensation {

    @Id
    private String id;

    private String creditId;

    private int installmentNumber;

    private BigDecimal amount;

    private Instant timestamp;

    @Indexed(unique = true)
    private String idempotencyKey;
}
