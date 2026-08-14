package com.bootcamp.creditservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Cliente HTTP hacia account-service, para debitar/compensar la cuenta origen de un pago de
 * credito de terceros. */
@Configuration
public class AccountServiceWebClientConfig {

    @Bean
    public WebClient accountServiceWebClient(
            @Value("${account-service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
