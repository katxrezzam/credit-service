package com.bootcamp.creditservice.model;

/** Copia local del tipo de cliente de customer-service - mismo criterio que account-service:
 * cada microservicio es un bounded context independiente, no se comparte codigo entre repos. */
public enum CustomerType {
    PERSONAL,
    BUSINESS
}
