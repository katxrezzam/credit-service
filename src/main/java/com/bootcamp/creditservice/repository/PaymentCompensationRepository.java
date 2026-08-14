package com.bootcamp.creditservice.repository;

import com.bootcamp.creditservice.model.PaymentCompensation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/** Repositorio reactivo de {@link PaymentCompensation}. */
public interface PaymentCompensationRepository
        extends ReactiveMongoRepository<PaymentCompensation, String> {

    Mono<PaymentCompensation> findByIdempotencyKey(String idempotencyKey);
}
