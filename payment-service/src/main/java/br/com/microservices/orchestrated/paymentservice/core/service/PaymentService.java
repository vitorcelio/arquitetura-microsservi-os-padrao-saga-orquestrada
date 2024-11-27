package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.EventDTO;
import br.com.microservices.orchestrated.paymentservice.core.dto.HistoryDTO;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProductDTO;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.*;

@Slf4j
@AllArgsConstructor
@Service
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_AMOUNT_VALUE = 0.1;

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final PaymentRepository repository;

    public void realizePayment(EventDTO event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);
            var payment = findByOrderIdAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            changePaymentToSuccess(payment);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Erro ao tentar realizar pagamento: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(EventDTO event) {
        if (repository.existsByOrderIdAndTransactionId(event.getPayload().getId(), event.getPayload().getTransactionId())) {
            throw new ValidationException("Já existe uma transação aberta para essa validação.");
        }
    }

    private void validateAmount(double amount) {
        if (amount < MIN_AMOUNT_VALUE) {
            throw new ValidationException("O valor deve ser, no mínimo, ".concat(String.valueOf(MIN_AMOUNT_VALUE)));
        }
    }

    private void createPendingPayment(EventDTO event) {

        var totalAmount = calculateAmount(event);
        var totalItems = calculateTotalItems(event);

        var payment = Payment.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getPayload().getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();

        save(payment);
        setEventAmountItems(event, payment);
    }

    private double calculateAmount(EventDTO event) {
        return event.getPayload()
                .getProducts()
                .stream()
                .map(prod -> prod.getQuantity() * prod.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private int calculateTotalItems(EventDTO event) {
        return event.getPayload()
                .getProducts()
                .stream()
                .map(OrderProductDTO::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    private Payment findByOrderIdAndTransactionId(EventDTO event) {
        return repository
                .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getPayload().getTransactionId())
                .orElseThrow(() -> new ValidationException("Pagamento não encontrado pelo OrderId a TransactionId"));
    }

    private void changePaymentToSuccess(Payment payment) {
        payment.setStatus(EPaymentStatus.SUCCESS);
        save(payment);
    }

    private void save(Payment payment) {
        repository.save(payment);
    }

    private void setEventAmountItems(EventDTO event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }

    private void handleSuccess(EventDTO event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Pagamento realizado com sucesso.");
    }

    private void addHistory(EventDTO event, String message) {
        var history = HistoryDTO.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        event.addToHistory(history);
    }

    public void realizeRefund(EventDTO event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);

        try {
            changePaymentStatusRefund(event);
            addHistory(event, "Roolback realizado para pagamento.");
        } catch (Exception e) {
            addHistory(event, "Rollback não executado para pagamento: ".concat(e.getMessage()));
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void changePaymentStatusRefund(EventDTO event) {
        Payment payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(EPaymentStatus.REFUND);
        setEventAmountItems(event, payment);
        save(payment);
    }

    private void handleFailCurrentNotExecuted(EventDTO event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Falha ao realizar pagamento: ".concat(message));
    }

}
