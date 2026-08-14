package com.bootcamp.creditservice.repository;

import com.bootcamp.creditservice.model.Credit;
import com.bootcamp.creditservice.model.CreditStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Repositorio reactivo de {@link Credit}. */
public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {

    /** Personal: solo bloquea si ya tiene un credito ACTIVE (D8, decidido con el usuario). */
    Mono<Boolean> existsByCustomerIdAndStatus(String customerId, CreditStatus status);

    /** Creditos de un cliente - usado por report-service via GET /credits?customerId= para el
     * reporte general por cliente. */
    Flux<Credit> findByCustomerId(String customerId);
}
