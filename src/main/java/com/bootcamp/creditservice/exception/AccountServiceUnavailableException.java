package com.bootcamp.creditservice.exception;

/**
 * account-service no respondio (caido, timeout, error 5xx, circuito abierto) mientras se
 * debitaba/compensaba la cuenta origen de un pago de credito. Mismo criterio que
 * CustomerServiceUnavailableException: se traduce a un 503 claro en vez de colgar la request.
 */
public class AccountServiceUnavailableException extends RuntimeException {
    public AccountServiceUnavailableException(String accountId, Throwable cause) {
        super("No se pudo debitar/compensar la cuenta " + accountId
                + " contra account-service", cause);
    }
}
