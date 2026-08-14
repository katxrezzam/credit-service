package com.bootcamp.creditservice.service;

import com.bootcamp.creditservice.dto.CreditRequest;
import com.bootcamp.creditservice.dto.CreditResponse;
import com.bootcamp.creditservice.dto.CreditUpdateRequest;
import com.bootcamp.creditservice.dto.PaymentRequest;
import com.bootcamp.creditservice.dto.PaymentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Casos de uso de negocio sobre creditos: CRUD + pagos de cuotas. */
public interface CreditService {

    Mono<CreditResponse> create(CreditRequest request);

    Flux<CreditResponse> findAll();

    Mono<CreditResponse> findById(String id);

    Mono<CreditResponse> update(String id, CreditUpdateRequest request);

    Mono<Void> delete(String id);

    Flux<PaymentResponse> findPayments(String creditId);

    Mono<PaymentResponse> pay(String creditId, PaymentRequest request, String idempotencyKey);
}
