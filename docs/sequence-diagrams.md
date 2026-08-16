# Diagramas de secuencia — credit-service

Requerimiento no funcional (Parte I): *"Elaborar diagramas de secuencia de cada microservicio."*

## Alta de crédito (un crédito activo por persona)

```mermaid
sequenceDiagram
    actor Cliente
    participant GW as api-gateway
    participant CrS as credit-service
    participant CustS as customer-service
    participant Mongo as creditdb

    Cliente->>GW: POST /credits
    GW->>CrS: forward
    CrS->>CustS: GET /customers/{id} (CustomerClient, circuit breaker)
    CustS-->>CrS: CustomerInfo
    alt PERSONAL con crédito ACTIVE existente
        CrS->>Mongo: existsByCustomerIdAndStatus(ACTIVE)
        Mongo-->>CrS: true
        CrS-->>GW: 400 "un crédito por persona"
    else habilitado (o BUSINESS, sin límite)
        Mongo-->>CrS: false
        CrS->>CrS: genera cuotas iguales (totalAmount/termMonths, redondeo en la última)
        CrS->>Mongo: save(Credit + installments)
        Mongo-->>CrS: crédito creado
        CrS-->>GW: 201 Created
    end
    GW-->>Cliente: respuesta
```

## Pago de cuota (propia o de un tercero) con débito real en account-service

Primera Saga cross-servicio del proyecto vía REST síncrono: si el retiro en `account-service` ya
se aplicó pero el registro local del pago falla después, se compensa con un depósito de
reversión y se bloquea el reintento con la misma clave (`PaymentCompensation`) para no aplicar el
pago local "gratis" una segunda vez.

```mermaid
sequenceDiagram
    actor Cliente
    participant GW as api-gateway
    participant CrS as credit-service
    participant AS as account-service
    participant Mongo as creditdb

    Cliente->>GW: POST /credits/{id}/payments (sourceAccountId, Idempotency-Key)
    GW->>CrS: forward
    CrS->>Mongo: findByIdempotencyKey(key)
    Mongo-->>CrS: vacío (clave nueva)
    CrS->>CrS: identifica la cuota impaga más antigua, valida monto exacto
    CrS->>AS: POST /accounts/{sourceAccountId}/withdrawals (AccountClient, circuit breaker)
    Note right of CrS: no valida ownership de sourceAccountId -<br/>permite pagar deuda de terceros (Fase III)
    AS-->>CrS: 201 (retiro real aplicado)
    CrS->>Mongo: save(Payment)
    alt guardado local falla después del retiro
        CrS->>AS: POST /accounts/{sourceAccountId}/deposits (compensación)
        AS-->>CrS: 201 (retiro revertido)
        CrS->>Mongo: save(PaymentCompensation) - rechaza reintentos con esta clave
        CrS-->>GW: 500 (fallo técnico, ya compensado)
    else guardado exitoso
        Mongo-->>CrS: Payment guardado
        CrS-->>GW: 201 Created
    end
    GW-->>Cliente: respuesta
```

## Consulta de deuda vencida (bloqueo de productos nuevos, Fase III)

```mermaid
sequenceDiagram
    participant AS as account-service
    participant CardS as card-service
    participant CrS as credit-service
    participant Mongo as creditdb

    Note over AS,CardS: antes de crear una cuenta o tarjeta nueva
    AS->>CrS: GET /credits/customers/{id}/has-overdue-debt
    CardS->>CrS: GET /credits/customers/{id}/has-overdue-debt
    CrS->>Mongo: busca cuotas vencidas sin pagar (fecha límite superada)
    Mongo-->>CrS: cuotas vencidas (si las hay)
    CrS-->>AS: true/false
    CrS-->>CardS: true/false
```
