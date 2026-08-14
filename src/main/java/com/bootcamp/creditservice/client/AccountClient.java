package com.bootcamp.creditservice.client;

import com.bootcamp.creditservice.dto.AccountErrorInfo;
import com.bootcamp.creditservice.dto.AccountMovementRequest;
import com.bootcamp.creditservice.exception.AccountDebitRejectedException;
import com.bootcamp.creditservice.exception.AccountServiceUnavailableException;
import java.math.BigDecimal;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente REST hacia account-service para debitar la cuenta origen de un pago de credito de
 * terceros, y para compensar (revertir) ese debito si el pago no se puede aplicar localmente
 * despues (Saga local con compensacion, D6/D7 - la primera vez que cruza servicios, antes vivia
 * entero dentro de account-service para transferencias).
 *
 * <p>Reutiliza tal cual los endpoints publicos POST /accounts/{id}/withdrawals y /deposits: el
 * pago hereda todas las reglas de la cuenta origen (fondos, limite de movimientos con comision
 * por exceso, dia de plazo fijo) en vez de duplicarlas.
 */
@Component
public class AccountClient {

    private static final String CIRCUIT_BREAKER_ID = "account-service";

    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public AccountClient(
            WebClient accountServiceWebClient,
            ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = accountServiceWebClient;
        this.circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_ID);
    }

    /** Debita amount de accountId. Emite AccountDebitRejectedException (con el status/mensaje
     * originales de account-service) o AccountServiceUnavailableException. */
    public Mono<Void> withdraw(String accountId, BigDecimal amount, String idempotencyKey) {
        return executeMovement(accountId, "withdrawals", amount, idempotencyKey);
    }

    /** Acredita amount a accountId - se usa solo para compensar un debito ya aplicado. */
    public Mono<Void> deposit(String accountId, BigDecimal amount, String idempotencyKey) {
        return executeMovement(accountId, "deposits", amount, idempotencyKey);
    }

    private Mono<Void> executeMovement(
            String accountId, String operation, BigDecimal amount, String idempotencyKey) {
        Mono<Void> call = webClient.post()
                .uri("/accounts/{id}/{operation}", accountId, operation)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(new AccountMovementRequest(amount))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response
                        .bodyToMono(AccountErrorInfo.class)
                        .flatMap(error -> Mono.error(new AccountDebitRejectedException(
                                HttpStatus.valueOf(error.status()), error.message()))))
                .toBodilessEntity()
                .then();

        return circuitBreaker.run(call, throwable -> mapFallback(accountId, throwable));
    }

    private Mono<Void> mapFallback(String accountId, Throwable throwable) {
        if (throwable instanceof AccountDebitRejectedException) {
            return Mono.error(throwable);
        }
        return Mono.error(new AccountServiceUnavailableException(accountId, throwable));
    }
}
