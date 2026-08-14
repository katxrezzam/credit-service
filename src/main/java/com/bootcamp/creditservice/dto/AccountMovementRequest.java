package com.bootcamp.creditservice.dto;

import java.math.BigDecimal;

/** Body de salida hacia POST /accounts/{id}/withdrawals y /deposits de account-service. */
public record AccountMovementRequest(BigDecimal amount) {
}
