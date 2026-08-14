package com.bootcamp.creditservice.service;

import com.bootcamp.creditservice.client.CustomerClient;
import com.bootcamp.creditservice.dto.CreditMapper;
import com.bootcamp.creditservice.dto.CreditRequest;
import com.bootcamp.creditservice.dto.CreditResponse;
import com.bootcamp.creditservice.dto.CreditUpdateRequest;
import com.bootcamp.creditservice.dto.PaymentMapper;
import com.bootcamp.creditservice.dto.PaymentRequest;
import com.bootcamp.creditservice.dto.PaymentResponse;
import com.bootcamp.creditservice.exception.CreditNotFoundException;
import com.bootcamp.creditservice.exception.InvalidBusinessRuleException;
import com.bootcamp.creditservice.model.Credit;
import com.bootcamp.creditservice.model.CreditStatus;
import com.bootcamp.creditservice.model.CustomerType;
import com.bootcamp.creditservice.model.Installment;
import com.bootcamp.creditservice.model.Payment;
import com.bootcamp.creditservice.repository.CreditRepository;
import com.bootcamp.creditservice.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementacion de {@link CreditService}. Mismas lecciones aprendidas que account-service:
 * validaciones diferidas con Mono.defer, cambios de estado sobre saldo/cuotas via update atomico
 * de Mongo (no read-modify-write en Java), idempotencia obligatoria en pagos.
 */
@Service
public class CreditServiceImpl implements CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditServiceImpl.class);

    private final CreditRepository creditRepository;
    private final PaymentRepository paymentRepository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final CustomerClient customerClient;

    public CreditServiceImpl(
            CreditRepository creditRepository,
            PaymentRepository paymentRepository,
            ReactiveMongoTemplate mongoTemplate,
            CustomerClient customerClient) {
        this.creditRepository = creditRepository;
        this.paymentRepository = paymentRepository;
        this.mongoTemplate = mongoTemplate;
        this.customerClient = customerClient;
    }

    @Override
    public Mono<CreditResponse> create(CreditRequest request) {
        return customerClient.getCustomer(request.customerId())
                .flatMap(customerInfo -> validateOnePersonalCreditActive(request.customerId(), customerInfo.customerType()))
                .then(Mono.defer(() -> creditRepository.save(buildNewCredit(request))))
                .doOnNext(saved -> log.info("Credito creado id={} customerId={}", saved.getId(), saved.getCustomerId()))
                .map(CreditMapper::toResponse);
    }

    @Override
    public Flux<CreditResponse> findAll() {
        return creditRepository.findAll().map(CreditMapper::toResponse);
    }

    @Override
    public Mono<CreditResponse> findById(String id) {
        return findEntityById(id).map(CreditMapper::toResponse);
    }

    @Override
    public Mono<CreditResponse> update(String id, CreditUpdateRequest request) {
        return findEntityById(id)
                .flatMap(existing -> {
                    boolean anyPaid = existing.getInstallments().stream().anyMatch(Installment::isPaid);
                    if (anyPaid) {
                        return Mono.<Credit>error(new InvalidBusinessRuleException(
                                "No se puede modificar un credito que ya tiene cuotas pagadas"));
                    }
                    existing.setTotalAmount(request.totalAmount());
                    existing.setTermMonths(request.termMonths());
                    existing.setInstallmentAmount(installmentBaseAmount(request.totalAmount(), request.termMonths()));
                    existing.setInstallments(generateInstallments(request.totalAmount(), request.termMonths()));
                    return creditRepository.save(existing);
                })
                .doOnNext(saved -> log.info("Credito actualizado id={}", saved.getId()))
                .map(CreditMapper::toResponse);
    }

    @Override
    public Mono<Void> delete(String id) {
        return findEntityById(id)
                .flatMap(credit -> {
                    boolean anyPaid = credit.getInstallments().stream().anyMatch(Installment::isPaid);
                    if (anyPaid) {
                        return Mono.<Credit>error(new InvalidBusinessRuleException(
                                "No se puede eliminar un credito con cuotas ya pagadas"));
                    }
                    return Mono.just(credit);
                })
                .flatMap(creditRepository::delete)
                .doOnSuccess(v -> log.info("Credito eliminado id={}", id));
    }

    @Override
    public Flux<PaymentResponse> findPayments(String creditId) {
        return findEntityById(creditId)
                .flatMapMany(credit -> paymentRepository.findByCreditId(creditId))
                .map(PaymentMapper::toResponse);
    }

    @Override
    public Mono<PaymentResponse> pay(String creditId, PaymentRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new InvalidBusinessRuleException(
                    "El header Idempotency-Key es obligatorio para pagar una cuota"));
        }
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(PaymentMapper::toResponse)
                .switchIfEmpty(Mono.defer(() -> executePayment(creditId, request, idempotencyKey)));
    }

    private Mono<PaymentResponse> executePayment(String creditId, PaymentRequest request, String idempotencyKey) {
        return findEntityById(creditId)
                .flatMap(credit -> validateAndFindTargetInstallment(credit, request.amount()))
                .flatMap(targetNumber -> applyInstallmentPayment(creditId, targetNumber)
                        .flatMap(this::markCreditPaidIfComplete)
                        .flatMap(updatedCredit -> recordPayment(updatedCredit, targetNumber, request.amount(), idempotencyKey)))
                .doOnNext(payment -> log.info("Pago registrado creditId={} cuota={}", creditId, payment.getInstallmentNumber()))
                .map(PaymentMapper::toResponse);
    }

    private Mono<Credit> findEntityById(String id) {
        return creditRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CreditNotFoundException(id))));
    }

    private Mono<Void> validateOnePersonalCreditActive(String customerId, CustomerType customerType) {
        if (customerType != CustomerType.PERSONAL) {
            return Mono.empty();
        }
        return creditRepository.existsByCustomerIdAndStatus(customerId, CreditStatus.ACTIVE)
                .flatMap(exists -> exists
                        ? Mono.<Void>error(new InvalidBusinessRuleException(
                                "El cliente ya tiene un credito activo (personal: uno por vez)"))
                        : Mono.empty());
    }

    private Credit buildNewCredit(CreditRequest request) {
        return Credit.builder()
                .customerId(request.customerId())
                .totalAmount(request.totalAmount())
                .termMonths(request.termMonths())
                .installmentAmount(installmentBaseAmount(request.totalAmount(), request.termMonths()))
                .status(CreditStatus.ACTIVE)
                .installments(generateInstallments(request.totalAmount(), request.termMonths()))
                .build();
    }

    private BigDecimal installmentBaseAmount(BigDecimal totalAmount, int termMonths) {
        return totalAmount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.DOWN);
    }

    /**
     * Cuotas iguales (monto total / plazo), con la diferencia de redondeo absorbida en la ultima
     * cuota para que la suma de todas cierre exacta contra totalAmount - nada de perder centavos
     * por redondeo, inaceptable con dinero real.
     */
    private List<Installment> generateInstallments(BigDecimal totalAmount, int termMonths) {
        BigDecimal baseAmount = installmentBaseAmount(totalAmount, termMonths);
        BigDecimal allocatedBeforeLast = baseAmount.multiply(BigDecimal.valueOf(termMonths - 1L));
        BigDecimal lastAmount = totalAmount.subtract(allocatedBeforeLast);

        List<Installment> installments = new ArrayList<>();
        LocalDate dueDate = LocalDate.now().plusMonths(1);
        for (int number = 1; number <= termMonths; number++) {
            BigDecimal amount = (number == termMonths) ? lastAmount : baseAmount;
            installments.add(Installment.builder()
                    .number(number)
                    .amount(amount)
                    .dueDate(dueDate)
                    .paid(false)
                    .build());
            dueDate = dueDate.plusMonths(1);
        }
        return installments;
    }

    /** Encuentra la cuota impaga mas antigua y valida que el monto pagado coincida exacto. */
    private Mono<Integer> validateAndFindTargetInstallment(Credit credit, BigDecimal amount) {
        return Mono.defer(() -> {
            if (credit.getStatus() == CreditStatus.PAID) {
                return Mono.error(new InvalidBusinessRuleException("El credito ya esta pagado en su totalidad"));
            }
            Optional<Installment> oldestUnpaid = credit.getInstallments().stream()
                    .filter(installment -> !installment.isPaid())
                    .min(Comparator.comparingInt(Installment::getNumber));
            if (oldestUnpaid.isEmpty()) {
                return Mono.error(new InvalidBusinessRuleException("No hay cuotas pendientes"));
            }
            Installment target = oldestUnpaid.get();
            if (amount.compareTo(target.getAmount()) != 0) {
                return Mono.error(new InvalidBusinessRuleException(
                        "El monto no coincide con la cuota pendiente #" + target.getNumber()
                                + " (" + target.getAmount() + ")"));
            }
            return Mono.just(target.getNumber());
        });
    }

    /**
     * Update atomico sobre el elemento del array que matchea numero+no-pagada (operador
     * posicional $): si dos pagos concurrentes apuntaran a la misma cuota, el segundo no
     * encuentra el documento (ya no matchea "paid: false") en vez de pisar al primero.
     */
    private Mono<Credit> applyInstallmentPayment(String creditId, int installmentNumber) {
        Query query = Query.query(Criteria.where("_id").is(creditId)
                .and("installments").elemMatch(
                        Criteria.where("number").is(installmentNumber).and("paid").is(false)));
        Update update = new Update()
                .set("installments.$.paid", true)
                .set("installments.$.paidAt", Instant.now())
                .currentDate("updatedAt");
        return mongoTemplate
                .findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Credit.class)
                .switchIfEmpty(Mono.error(new InvalidBusinessRuleException(
                        "No se pudo aplicar el pago (probable pago concurrente), reintente")));
    }

    private Mono<Credit> markCreditPaidIfComplete(Credit credit) {
        boolean allPaid = credit.getInstallments().stream().allMatch(Installment::isPaid);
        if (!allPaid || credit.getStatus() == CreditStatus.PAID) {
            return Mono.just(credit);
        }
        credit.setStatus(CreditStatus.PAID);
        return creditRepository.save(credit);
    }

    private Mono<Payment> recordPayment(Credit credit, int installmentNumber, BigDecimal amount, String idempotencyKey) {
        Payment payment = Payment.builder()
                .creditId(credit.getId())
                .installmentNumber(installmentNumber)
                .amount(amount)
                .timestamp(Instant.now())
                .idempotencyKey(idempotencyKey)
                .build();
        return paymentRepository.save(payment);
    }
}
