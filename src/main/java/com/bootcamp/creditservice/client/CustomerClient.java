package com.bootcamp.creditservice.client;

import com.bootcamp.creditservice.dto.CustomerInfo;
import com.bootcamp.creditservice.exception.CustomerNotFoundException;
import com.bootcamp.creditservice.exception.CustomerServiceUnavailableException;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public CustomerClient(WebClient customerServiceWebClient) {
        this.webClient = customerServiceWebClient;
    }

    public Mono<CustomerInfo> getCustomer(String customerId) {
        return webClient.get()
                .uri("/customers/{id}", customerId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new CustomerNotFoundException(customerId)))
                .bodyToMono(CustomerInfo.class)
                .timeout(TIMEOUT)
                .onErrorMap(
                        ex -> !(ex instanceof CustomerNotFoundException),
                        ex -> new CustomerServiceUnavailableException(customerId, ex));
    }
}
