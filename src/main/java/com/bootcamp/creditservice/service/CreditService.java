package com.bootcamp.creditservice.service;

import com.bootcamp.creditservice.dto.CreditRequest;
import com.bootcamp.creditservice.dto.CreditResponse;
import com.bootcamp.creditservice.dto.CreditUpdateRequest;
import com.bootcamp.creditservice.dto.PaymentRequest;
import com.bootcamp.creditservice.dto.PaymentResponse;
import java.time.Instant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Casos de uso de negocio sobre creditos: CRUD + pagos de cuotas. */
public interface CreditService {

    Mono<CreditResponse> create(CreditRequest request);

    /** customerId es opcional: si viene, filtra a los creditos de ese cliente (usado por
     * report-service); si es null, se comporta como antes (todos los creditos). */
    Flux<CreditResponse> findAll(String customerId);

    Mono<CreditResponse> findById(String id);

    Mono<CreditResponse> update(String id, CreditUpdateRequest request);

    Mono<Void> delete(String id);

    /** from/to son opcionales: si ambos vienen null, se comporta como antes (todo el
     * historial); usado por report-service para el reporte general en un intervalo. */
    Flux<PaymentResponse> findPayments(String creditId, Instant from, Instant to);

    Mono<PaymentResponse> pay(String creditId, PaymentRequest request, String idempotencyKey);
}
