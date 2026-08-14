package com.bootcamp.creditservice.repository;

import com.bootcamp.creditservice.model.Payment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Repositorio reactivo de {@link Payment}. */
public interface PaymentRepository extends ReactiveMongoRepository<Payment, String> {

    Flux<Payment> findByCreditId(String creditId);

    /** Idempotencia: mismo criterio que Movement en account-service. */
    Mono<Payment> findByIdempotencyKey(String idempotencyKey);
}
