package com.bootcamp.creditservice.exception;

/** Se lanza cuando no existe un credito con el id pedido. Mapea a HTTP 404. */
public class CreditNotFoundException extends RuntimeException {
    public CreditNotFoundException(String id) {
        super("No existe un credito con id " + id);
    }
}
