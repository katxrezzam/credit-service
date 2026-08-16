package com.bootcamp.creditservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de credit-service: creditos con cuotas y pagos idempotentes. Contrato
 * OpenAPI generado en /v3/api-docs, explorable en /swagger-ui.html. */
@OpenAPIDefinition(info = @Info(
        title = "credit-service",
        version = "v1",
        description = "Creditos personal (uno por persona) y empresarial (varios): cuotas "
                + "iguales autogeneradas, pagos idempotentes incluido pago de deuda de terceros."))
@SpringBootApplication
public class CreditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditServiceApplication.class, args);
    }
}
