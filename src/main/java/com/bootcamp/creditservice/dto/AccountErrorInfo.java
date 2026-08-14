package com.bootcamp.creditservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Subconjunto del ErrorResponse de account-service que a credit-service le interesa: el
 * status/mensaje original se reenvia tal cual al llamador de POST /credits/{id}/payments
 * (fondos insuficientes, cuenta inexistente, limite de movimientos, etc). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountErrorInfo(int status, String message) {
}
