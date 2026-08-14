package com.bootcamp.creditservice.exception;

/** Regla de negocio violada (monto de cuota, credito ya pagado, etc.). Mapea a HTTP 400. */
public class InvalidBusinessRuleException extends RuntimeException {
    public InvalidBusinessRuleException(String message) {
        super(message);
    }
}
