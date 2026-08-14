package com.bootcamp.creditservice.dto;

import com.bootcamp.creditservice.model.CustomerType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Subconjunto de GET /customers/{id} de customer-service que a credit-service le interesa. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerInfo(String id, CustomerType customerType) {
}
