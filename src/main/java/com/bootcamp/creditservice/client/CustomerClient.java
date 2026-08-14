package com.bootcamp.creditservice.client;

import com.bootcamp.creditservice.dto.CustomerInfo;
import com.bootcamp.creditservice.exception.CustomerNotFoundException;
import com.bootcamp.creditservice.exception.CustomerServiceUnavailableException;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente REST hacia customer-service, protegido con circuit breaker + timeout de 2s
 * (Resilience4j via Spring Cloud CircuitBreaker). Mismo patron validado en account-service:
 * CustomerNotFoundException (404 de negocio) esta declarada como ignore-exception en
 * bootcamp-config-repo/credit-service.yml, no cuenta como falla tecnica.
 */
@Component
public class CustomerClient {

    private static final String CIRCUIT_BREAKER_ID = "customer-service";

    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public CustomerClient(
            WebClient customerServiceWebClient,
            ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = customerServiceWebClient;
        this.circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_ID);
    }

    /** Trae el cliente por id. Emite CustomerNotFoundException (404) o
     * CustomerServiceUnavailableException (circuito abierto/timeout/otro error). */
    public Mono<CustomerInfo> getCustomer(String customerId) {
        Mono<CustomerInfo> call = webClient.get()
                .uri("/customers/{id}", customerId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new CustomerNotFoundException(customerId)))
                .bodyToMono(CustomerInfo.class);

        return circuitBreaker.run(call, throwable -> mapFallback(customerId, throwable));
    }

    private Mono<CustomerInfo> mapFallback(String customerId, Throwable throwable) {
        if (throwable instanceof CustomerNotFoundException) {
            return Mono.error(throwable);
        }
        return Mono.error(new CustomerServiceUnavailableException(customerId, throwable));
    }
}
