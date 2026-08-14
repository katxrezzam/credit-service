package com.bootcamp.creditservice.exception;

/** Sin Resilience4j todavia (Fase 2): se traduce a 503 claro en vez de colgar la request. */
public class CustomerServiceUnavailableException extends RuntimeException {
    public CustomerServiceUnavailableException(String customerId, Throwable cause) {
        super("No se pudo validar el cliente " + customerId + " contra customer-service", cause);
    }
}
