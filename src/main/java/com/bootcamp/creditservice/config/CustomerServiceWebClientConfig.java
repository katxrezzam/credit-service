package com.bootcamp.creditservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Cliente HTTP hacia customer-service, para validar clientes al crear un credito. */
@Configuration
public class CustomerServiceWebClientConfig {

    @Bean
    public WebClient customerServiceWebClient(
            @Value("${customer-service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
