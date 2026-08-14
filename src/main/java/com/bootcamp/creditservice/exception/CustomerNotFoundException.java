package com.bootcamp.creditservice.exception;

/** El customerId no corresponde a ningun cliente real en customer-service. Mapea a HTTP 400. */
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String customerId) {
        super("No existe un cliente con id " + customerId + " en customer-service");
    }
}
