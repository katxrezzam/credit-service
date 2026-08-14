package com.bootcamp.creditservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bootcamp.creditservice.dto.CreditRequest;
import com.bootcamp.creditservice.dto.CreditResponse;
import com.bootcamp.creditservice.exception.CreditNotFoundException;
import com.bootcamp.creditservice.exception.GlobalExceptionHandler;
import com.bootcamp.creditservice.model.CreditStatus;
import com.bootcamp.creditservice.service.CreditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = CreditController.class)
@Import(GlobalExceptionHandler.class)
class CreditControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreditService creditService;

    private CreditResponse sampleResponse() {
        return new CreditResponse("cred1", "cust1", new BigDecimal("300.00"), 3,
                new BigDecimal("100.00"), CreditStatus.ACTIVE, List.of(), Instant.now(), Instant.now());
    }

    @Test
    void post_requestValido_retorna201() {
        when(creditService.create(any(CreditRequest.class))).thenReturn(Mono.just(sampleResponse()));

        CreditRequest request = new CreditRequest("cust1", new BigDecimal("300.00"), 3);

        webTestClient.post().uri("/credits")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("cred1");
    }

    @Test
    void get_inexistente_retorna404() {
        when(creditService.findById(eq("no-existe"))).thenReturn(Mono.error(new CreditNotFoundException("no-existe")));

        webTestClient.get().uri("/credits/{id}", "no-existe")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.correlationId").exists();
    }

    @Test
    void getAll_retorna200() {
        when(creditService.findAll(org.mockito.ArgumentMatchers.any()))
                .thenReturn(reactor.core.publisher.Flux.just(sampleResponse()));

        webTestClient.get().uri("/credits")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CreditResponse.class)
                .hasSize(1);
    }

    @Test
    void hasOverdueDebt_true_retorna200ConTrue() {
        when(creditService.hasOverdueDebt("cust1")).thenReturn(Mono.just(true));

        webTestClient.get().uri("/credits/customers/{customerId}/has-overdue-debt", "cust1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(true);
    }

    @Test
    void rutaInexistente_retorna404NoQuinientos() {
        // El bug que encontramos con Newman: una URL que no matchea ningun endpoint debe dar 404
        // real, no 500 generico (fix en GlobalExceptionHandler via ErrorResponseException).
        webTestClient.get().uri("/credits/")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
