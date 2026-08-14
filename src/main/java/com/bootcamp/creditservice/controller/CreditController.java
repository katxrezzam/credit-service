package com.bootcamp.creditservice.controller;

import com.bootcamp.creditservice.dto.CreditRequest;
import com.bootcamp.creditservice.dto.CreditResponse;
import com.bootcamp.creditservice.dto.CreditUpdateRequest;
import com.bootcamp.creditservice.dto.PaymentRequest;
import com.bootcamp.creditservice.dto.PaymentResponse;
import com.bootcamp.creditservice.service.CreditService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Endpoints REST de creditos: CRUD y pagos de cuotas. */
@RestController
@RequestMapping("/credits")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreditResponse> create(@Valid @RequestBody CreditRequest request) {
        return creditService.create(request);
    }

    @GetMapping
    public Flux<CreditResponse> findAll(
            @RequestParam(required = false) String customerId) {
        return creditService.findAll(customerId);
    }

    @GetMapping("/{id}")
    public Mono<CreditResponse> findById(@PathVariable String id) {
        return creditService.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<CreditResponse> update(
            @PathVariable String id, @Valid @RequestBody CreditUpdateRequest request) {
        return creditService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return creditService.delete(id);
    }

    @GetMapping("/{id}/payments")
    public Flux<PaymentResponse> findPayments(
            @PathVariable String id,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return creditService.findPayments(id, from, to);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PaymentResponse> pay(
            @PathVariable String id,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return creditService.pay(id, request, idempotencyKey);
    }
}
