package com.bootcamp.creditservice.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String customerId) {
        super("No existe un cliente con id " + customerId + " en customer-service");
    }
}
