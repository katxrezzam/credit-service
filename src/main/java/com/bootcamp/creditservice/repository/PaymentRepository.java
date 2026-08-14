package com.bootcamp.creditservice.repository;

import com.bootcamp.creditservice.model.Payment;
import java.time.Instant;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Repositorio reactivo de {@link Payment}. */
public interface PaymentRepository extends ReactiveMongoRepository<Payment, String> {

    Flux<Payment> findByCreditId(String creditId);

    /** Usado por report-service (via GET /credits/{id}/payments?from=&to=) para el reporte
     * general por cliente en un intervalo. */
    Flux<Payment> findByCreditIdAndTimestampBetween(String creditId, Instant start, Instant end);

    /** Idempotencia: mismo criterio que Movement en account-service. */
    Mono<Payment> findByIdempotencyKey(String idempotencyKey);
}
