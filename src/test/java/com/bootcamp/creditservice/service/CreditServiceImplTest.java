package com.bootcamp.creditservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bootcamp.creditservice.client.AccountClient;
import com.bootcamp.creditservice.client.CustomerClient;
import com.bootcamp.creditservice.dto.CreditRequest;
import com.bootcamp.creditservice.dto.CreditUpdateRequest;
import com.bootcamp.creditservice.dto.CustomerInfo;
import com.bootcamp.creditservice.dto.PaymentRequest;
import com.bootcamp.creditservice.exception.AccountDebitRejectedException;
import com.bootcamp.creditservice.exception.AccountServiceUnavailableException;
import com.bootcamp.creditservice.exception.CreditNotFoundException;
import com.bootcamp.creditservice.exception.InvalidBusinessRuleException;
import com.bootcamp.creditservice.model.Credit;
import com.bootcamp.creditservice.model.CreditStatus;
import com.bootcamp.creditservice.model.CustomerType;
import com.bootcamp.creditservice.model.Installment;
import com.bootcamp.creditservice.model.Payment;
import com.bootcamp.creditservice.model.PaymentCompensation;
import com.bootcamp.creditservice.repository.CreditRepository;
import com.bootcamp.creditservice.repository.PaymentCompensationRepository;
import com.bootcamp.creditservice.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreditServiceImplTest {

    @Mock
    private CreditRepository creditRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentCompensationRepository paymentCompensationRepository;
    @Mock
    private ReactiveMongoTemplate mongoTemplate;
    @Mock
    private CustomerClient customerClient;
    @Mock
    private AccountClient accountClient;

    private CreditServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CreditServiceImpl(creditRepository, paymentRepository,
                paymentCompensationRepository, mongoTemplate, customerClient, accountClient);
    }

    private Installment installment(int number, String amount, boolean paid) {
        return Installment.builder()
                .number(number)
                .amount(new BigDecimal(amount))
                .dueDate(LocalDate.now().plusMonths(number))
                .paid(paid)
                .build();
    }

    private Credit activeCreditWithInstallments(List<Installment> installments) {
        return Credit.builder()
                .id("cred1")
                .customerId("cust1")
                .totalAmount(new BigDecimal("300.00"))
                .termMonths(3)
                .installmentAmount(new BigDecimal("100.00"))
                .status(CreditStatus.ACTIVE)
                .installments(installments)
                .build();
    }

    // ---------- create ----------

    @Test
    void create_personalSinCreditoActivo_creaConCuotas() {
        CreditRequest request = new CreditRequest("cust1", new BigDecimal("300.00"), 3);
        when(customerClient.getCustomer("cust1")).thenReturn(Mono.just(new CustomerInfo("cust1", CustomerType.PERSONAL)));
        when(creditRepository.existsByCustomerIdAndStatus("cust1", CreditStatus.ACTIVE)).thenReturn(Mono.just(false));
        when(creditRepository.findByCustomerId("cust1")).thenReturn(Flux.empty());

        ArgumentCaptor<Credit> captor = ArgumentCaptor.forClass(Credit.class);
        Credit saved = activeCreditWithInstallments(List.of(
                installment(1, "100.00", false), installment(2, "100.00", false), installment(3, "100.00", false)));
        when(creditRepository.save(captor.capture())).thenReturn(Mono.just(saved));

        StepVerifier.create(service.create(request))
                .expectNextMatches(response -> response.installments().size() == 3
                        && response.totalAmount().compareTo(new BigDecimal("300.00")) == 0)
                .verifyComplete();

        Credit toSave = captor.getValue();
        BigDecimal sum = toSave.getInstallments().stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        org.junit.jupiter.api.Assertions.assertEquals(0, sum.compareTo(new BigDecimal("300.00")),
                "la suma de las cuotas debe cerrar exacta contra totalAmount");
    }

    @Test
    void create_cuotasConRedondeo_laUltimaAbsorbeLaDiferencia() {
        // 100 / 3 = 33.33 (DOWN), 3 cuotas -> 33.33 + 33.33 + 33.34 = 100.00 exacto
        CreditRequest request = new CreditRequest("cust1", new BigDecimal("100.00"), 3);
        when(customerClient.getCustomer("cust1")).thenReturn(Mono.just(new CustomerInfo("cust1", CustomerType.PERSONAL)));
        when(creditRepository.existsByCustomerIdAndStatus("cust1", CreditStatus.ACTIVE)).thenReturn(Mono.just(false));
        when(creditRepository.findByCustomerId("cust1")).thenReturn(Flux.empty());

        ArgumentCaptor<Credit> captor = ArgumentCaptor.forClass(Credit.class);
        when(creditRepository.save(captor.capture())).thenAnswer(inv -> Mono.just(captor.getValue()));

        StepVerifier.create(service.create(request)).expectNextCount(1).verifyComplete();

        List<Installment> installments = captor.getValue().getInstallments();
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("33.33"), installments.get(0).getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("33.33"), installments.get(1).getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("33.34"), installments.get(2).getAmount());
    }

    @Test
    void create_personalConCreditoActivo_falla() {
        CreditRequest request = new CreditRequest("cust1", new BigDecimal("300.00"), 3);
        when(customerClient.getCustomer("cust1")).thenReturn(Mono.just(new CustomerInfo("cust1", CustomerType.PERSONAL)));
        when(creditRepository.existsByCustomerIdAndStatus("cust1", CreditStatus.ACTIVE)).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(request))
                .expectError(InvalidBusinessRuleException.class)
                .verify();
    }

    @Test
    void create_businessSinLimite_permiteVariosCreditos() {
        CreditRequest request = new CreditRequest("bus1", new BigDecimal("300.00"), 3);
        when(customerClient.getCustomer("bus1")).thenReturn(Mono.just(new CustomerInfo("bus1", CustomerType.BUSINESS)));
        when(creditRepository.findByCustomerId("bus1")).thenReturn(Flux.empty());
        when(creditRepository.save(any(Credit.class))).thenReturn(Mono.just(activeCreditWithInstallments(List.of())));

        StepVerifier.create(service.create(request)).expectNextCount(1).verifyComplete();

        verify(creditRepository, org.mockito.Mockito.never()).existsByCustomerIdAndStatus(any(), any());
    }

    @Test
    void create_conDeudaVencida_falla() {
        CreditRequest request = new CreditRequest("cust1", new BigDecimal("300.00"), 3);
        Credit creditoConCuotaVencida = activeCreditWithInstallments(
                List.of(Installment.builder().number(1).amount(new BigDecimal("100.00"))
                        .dueDate(LocalDate.now().minusDays(1)).paid(false).build()));
        when(customerClient.getCustomer("cust1")).thenReturn(Mono.just(new CustomerInfo("cust1", CustomerType.PERSONAL)));
        when(creditRepository.existsByCustomerIdAndStatus("cust1", CreditStatus.ACTIVE)).thenReturn(Mono.just(false));
        when(creditRepository.findByCustomerId("cust1")).thenReturn(Flux.just(creditoConCuotaVencida));

        StepVerifier.create(service.create(request))
                .expectError(InvalidBusinessRuleException.class)
                .verify();

        verify(creditRepository, org.mockito.Mockito.never()).save(any());
    }

    // ---------- findById / update / delete ----------

    @Test
    void findById_inexistente_falla() {
        when(creditRepository.findById("no-existe")).thenReturn(Mono.empty());

        StepVerifier.create(service.findById("no-existe"))
                .expectError(CreditNotFoundException.class)
                .verify();
    }

    @Test
    void update_conCuotaPagada_falla() {
        Credit existing = activeCreditWithInstallments(List.of(installment(1, "100.00", true), installment(2, "100.00", false)));
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.update("cred1", new CreditUpdateRequest(new BigDecimal("500.00"), 5)))
                .expectError(InvalidBusinessRuleException.class)
                .verify();
    }

    @Test
    void update_sinCuotasPagadas_regeneraElCronograma() {
        Credit existing = activeCreditWithInstallments(List.of(installment(1, "100.00", false), installment(2, "100.00", false), installment(3, "100.00", false)));
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(existing));
        when(creditRepository.save(any(Credit.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.update("cred1", new CreditUpdateRequest(new BigDecimal("600.00"), 6)))
                .expectNextMatches(response -> response.termMonths() == 6
                        && response.installments().size() == 6
                        && response.totalAmount().compareTo(new BigDecimal("600.00")) == 0)
                .verifyComplete();
    }

    @Test
    void delete_conCuotaPagada_falla() {
        Credit existing = activeCreditWithInstallments(List.of(installment(1, "100.00", true)));
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.delete("cred1"))
                .expectError(InvalidBusinessRuleException.class)
                .verify();
    }

    @Test
    void delete_sinCuotasPagadas_elimina() {
        Credit existing = activeCreditWithInstallments(List.of(installment(1, "100.00", false)));
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(existing));
        when(creditRepository.delete(existing)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete("cred1")).verifyComplete();
    }

    // ---------- pay ----------

    @Test
    void pay_sinIdempotencyKey_falla() {
        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), null))
                .expectError(InvalidBusinessRuleException.class)
                .verify();
    }

    @Test
    void pay_conClaveYaUsada_devuelveElPagoExistenteSinReaplicar() {
        Payment existing = Payment.builder().id("pay1").creditId("cred1").installmentNumber(1)
                .amount(new BigDecimal("100.00")).timestamp(Instant.now()).idempotencyKey("key-1").build();
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-1"))
                .expectNextMatches(response -> "pay1".equals(response.id()))
                .verifyComplete();

        verify(accountClient, org.mockito.Mockito.never()).withdraw(any(), any(), any());
    }

    @Test
    void pay_reintentoConClaveYaCompensada_fallaExplicito() {
        PaymentCompensation existing = PaymentCompensation.builder().id("comp1").creditId("cred1")
                .installmentNumber(1).amount(new BigDecimal("100.00")).timestamp(Instant.now())
                .idempotencyKey("key-compensada").build();
        when(paymentRepository.findByIdempotencyKey("key-compensada")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-compensada")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-compensada"))
                .expectError(InvalidBusinessRuleException.class)
                .verify();

        verify(accountClient, org.mockito.Mockito.never()).withdraw(any(), any(), any());
        verify(creditRepository, org.mockito.Mockito.never()).findById(any(String.class));
    }

    @Test
    void pay_montoNoCoincideConLaCuota_falla() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", false), installment(2, "100.00", false)));
        when(paymentRepository.findByIdempotencyKey("key-2")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-2")).thenReturn(Mono.empty());
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("50.00"), "acc1"), "key-2"))
                .expectError(InvalidBusinessRuleException.class)
                .verify();

        verify(accountClient, org.mockito.Mockito.never()).withdraw(any(), any(), any());
    }

    @Test
    void pay_valido_debitaLaCuentaOrigenYPagaLaCuotaMasAntigua() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", false), installment(2, "100.00", false)));
        Credit afterPayment = activeCreditWithInstallments(List.of(installment(1, "100.00", true), installment(2, "100.00", false)));
        Payment savedPayment = Payment.builder().id("pay2").creditId("cred1").installmentNumber(1)
                .amount(new BigDecimal("100.00")).timestamp(Instant.now()).idempotencyKey("key-3").build();

        when(paymentRepository.findByIdempotencyKey("key-3")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-3")).thenReturn(Mono.empty());
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));
        when(accountClient.withdraw("acc1", new BigDecimal("100.00"), "key-3:withdrawal")).thenReturn(Mono.empty());
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Credit.class)))
                .thenReturn(Mono.just(afterPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(savedPayment));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-3"))
                .expectNextMatches(response -> response.installmentNumber() == 1 && "pay2".equals(response.id()))
                .verifyComplete();

        verify(accountClient).withdraw("acc1", new BigDecimal("100.00"), "key-3:withdrawal");
    }

    @Test
    void pay_fondosInsuficientes_propagaElRechazoSinAplicarNadaLocal() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", false)));
        when(paymentRepository.findByIdempotencyKey("key-insuf")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-insuf")).thenReturn(Mono.empty());
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));
        when(accountClient.withdraw("acc1", new BigDecimal("100.00"), "key-insuf:withdrawal"))
                .thenReturn(Mono.error(new AccountDebitRejectedException(
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente")));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-insuf"))
                .expectError(AccountDebitRejectedException.class)
                .verify();

        verify(mongoTemplate, org.mockito.Mockito.never())
                .findAndModify(any(), any(), any(), eq(Credit.class));
        verify(accountClient, org.mockito.Mockito.never()).deposit(any(), any(), any());
    }

    @Test
    void pay_accountServiceCaido_propagaError() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", false)));
        when(paymentRepository.findByIdempotencyKey("key-caido")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-caido")).thenReturn(Mono.empty());
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));
        when(accountClient.withdraw("acc1", new BigDecimal("100.00"), "key-caido:withdrawal"))
                .thenReturn(Mono.error(new AccountServiceUnavailableException("acc1", new RuntimeException("timeout"))));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-caido"))
                .expectError(AccountServiceUnavailableException.class)
                .verify();
    }

    @Test
    void pay_fallaLocalTrasDebitar_compensaYDejaConstanciaParaRechazarReintento() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", false)));
        when(paymentRepository.findByIdempotencyKey("key-comp")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-comp")).thenReturn(Mono.empty());
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));
        when(accountClient.withdraw("acc1", new BigDecimal("100.00"), "key-comp:withdrawal")).thenReturn(Mono.empty());
        // findAndModify no encuentra el documento (pago concurrente) -> applyInstallmentPayment falla
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Credit.class)))
                .thenReturn(Mono.empty());
        when(accountClient.deposit("acc1", new BigDecimal("100.00"), "key-comp:compensation")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.save(any(PaymentCompensation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-comp"))
                .expectError(InvalidBusinessRuleException.class)
                .verify();

        verify(accountClient).deposit("acc1", new BigDecimal("100.00"), "key-comp:compensation");
        verify(paymentCompensationRepository).save(any(PaymentCompensation.class));
        verify(paymentRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void pay_ultimaCuota_marcaElCreditoComoPAID() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", true), installment(2, "100.00", false)));
        Credit afterPaymentAllPaid = activeCreditWithInstallments(List.of(installment(1, "100.00", true), installment(2, "100.00", true)));
        Payment savedPayment = Payment.builder().id("pay3").creditId("cred1").installmentNumber(2)
                .amount(new BigDecimal("100.00")).timestamp(Instant.now()).idempotencyKey("key-4").build();

        when(paymentRepository.findByIdempotencyKey("key-4")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-4")).thenReturn(Mono.empty());
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));
        when(accountClient.withdraw("acc1", new BigDecimal("100.00"), "key-4:withdrawal")).thenReturn(Mono.empty());
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Credit.class)))
                .thenReturn(Mono.just(afterPaymentAllPaid));
        when(creditRepository.save(afterPaymentAllPaid)).thenReturn(Mono.just(afterPaymentAllPaid));
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(savedPayment));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-4"))
                .expectNextMatches(response -> response.installmentNumber() == 2)
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(CreditStatus.PAID, afterPaymentAllPaid.getStatus());
    }

    @Test
    void pay_creditoYaPagado_falla() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", true)));
        credit.setStatus(CreditStatus.PAID);
        when(paymentRepository.findByIdempotencyKey("key-5")).thenReturn(Mono.empty());
        when(paymentCompensationRepository.findByIdempotencyKey("key-5")).thenReturn(Mono.empty());
        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));

        StepVerifier.create(service.pay("cred1", new PaymentRequest(new BigDecimal("100.00"), "acc1"), "key-5"))
                .expectError(InvalidBusinessRuleException.class)
                .verify();
    }

    // ---------- hasOverdueDebt ----------

    @Test
    void hasOverdueDebt_conCuotaVencidaSinPagar_devuelveTrue() {
        Credit credit = activeCreditWithInstallments(
                List.of(Installment.builder().number(1).amount(new BigDecimal("100.00"))
                        .dueDate(LocalDate.now().minusDays(1)).paid(false).build()));
        when(creditRepository.findByCustomerId("cust1")).thenReturn(Flux.just(credit));

        StepVerifier.create(service.hasOverdueDebt("cust1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasOverdueDebt_cuotaVencidaPeroYaPagada_devuelveFalse() {
        Credit credit = activeCreditWithInstallments(
                List.of(Installment.builder().number(1).amount(new BigDecimal("100.00"))
                        .dueDate(LocalDate.now().minusDays(1)).paid(true).build()));
        when(creditRepository.findByCustomerId("cust1")).thenReturn(Flux.just(credit));

        StepVerifier.create(service.hasOverdueDebt("cust1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasOverdueDebt_soloCuotasFuturasOAlDia_devuelveFalse() {
        Credit credit = activeCreditWithInstallments(List.of(installment(1, "100.00", false)));
        when(creditRepository.findByCustomerId("cust1")).thenReturn(Flux.just(credit));

        StepVerifier.create(service.hasOverdueDebt("cust1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasOverdueDebt_sinCreditos_devuelveFalse() {
        when(creditRepository.findByCustomerId("cust1")).thenReturn(Flux.empty());

        StepVerifier.create(service.hasOverdueDebt("cust1"))
                .expectNext(false)
                .verifyComplete();
    }

    // ---------- findAll / findPayments (report-service) ----------

    @Test
    void findAll_sinCustomerId_devuelveTodos() {
        when(creditRepository.findAll())
                .thenReturn(reactor.core.publisher.Flux.just(activeCreditWithInstallments(List.of())));

        StepVerifier.create(service.findAll(null))
                .expectNextCount(1)
                .verifyComplete();

        org.mockito.Mockito.verify(creditRepository, org.mockito.Mockito.never())
                .findByCustomerId(any());
    }

    @Test
    void findAll_conCustomerId_filtraPorCliente() {
        when(creditRepository.findByCustomerId("cust1"))
                .thenReturn(reactor.core.publisher.Flux.just(activeCreditWithInstallments(List.of())));

        StepVerifier.create(service.findAll("cust1"))
                .expectNextCount(1)
                .verifyComplete();

        org.mockito.Mockito.verify(creditRepository, org.mockito.Mockito.never()).findAll();
    }

    @Test
    void findPayments_conRangoDeFechas_filtraPorIntervalo() {
        Credit credit = activeCreditWithInstallments(List.of());
        Payment payment = Payment.builder().id("pay1").creditId("cred1").installmentNumber(1)
                .amount(new BigDecimal("100.00")).timestamp(Instant.now()).build();
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        when(creditRepository.findById("cred1")).thenReturn(Mono.just(credit));
        when(paymentRepository.findByCreditIdAndTimestampBetween("cred1", from, to))
                .thenReturn(reactor.core.publisher.Flux.just(payment));

        StepVerifier.create(service.findPayments("cred1", from, to))
                .expectNextCount(1)
                .verifyComplete();

        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.never())
                .findByCreditId(any());
    }
}
