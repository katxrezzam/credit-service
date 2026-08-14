package com.bootcamp.creditservice.exception;

public class CreditNotFoundException extends RuntimeException {
    public CreditNotFoundException(String id) {
        super("No existe un credito con id " + id);
    }
}
