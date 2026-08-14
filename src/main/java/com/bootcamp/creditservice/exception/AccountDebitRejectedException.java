package com.bootcamp.creditservice.exception;

import org.springframework.http.HttpStatus;

/**
 * account-service rechazo el debito/deposito con un error de negocio propio (fondos
 * insuficientes, cuenta inexistente, cuenta a plazo fijo fuera de su dia, etc). Se reenvia el
 * status y el mensaje originales de account-service tal cual, mismo criterio que
 * ErrorResponseException en GlobalExceptionHandler: respetar el status que ya trae el error en
 * vez de forzarlo a uno generico.
 */
public class AccountDebitRejectedException extends RuntimeException {

    private final HttpStatus status;

    public AccountDebitRejectedException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
