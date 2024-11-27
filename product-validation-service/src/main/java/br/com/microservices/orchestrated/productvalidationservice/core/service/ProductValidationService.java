package br.com.microservices.orchestrated.productvalidationservice.core.service;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.EventDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.HistoryDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProductDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.*;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final ProductRepository repository;
    private final ValidationRepository validationRepository;
    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;

    public void validateExistingProducts(EventDTO event) {
        try {
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Erro na validação do produto: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(EventDTO event) {
        validateProductsInformed(event);
        if (validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("Já existe uma transação aberto para essa validação.");
        }

        event.getPayload().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());
        });
    }

    private void validateProductsInformed(EventDTO event) {
        if (isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())) {
            throw new ValidationException("Lista de produtos vazia.");
        }

        if (isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())) {
            throw new ValidationException("OrderId ou TransactionId devem ser informados.");
        }
    }

    private void validateProductInformed(OrderProductDTO orderProduct) {
        if (isEmpty(orderProduct.getProduct()) || isEmpty(orderProduct.getProduct().getCode())) {
            throw new ValidationException("Produto deve ser informado");
        }
    }

    private void validateExistingProduct(String code) {
        if (!repository.existsByCode(code)) {
            throw new ValidationException("Produto não encontrado.");
        }
    }

    private void createValidation(EventDTO event, boolean success) {
        var validation = Validation.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getPayload().getTransactionId())
                .success(success)
                .build();

        validationRepository.save(validation);
    }

    private void handleSuccess(EventDTO event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Produtos validados com sucesso.");
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

    private void handleFailCurrentNotExecuted(EventDTO event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Falha ao validar produtos: ".concat(message));
    }

    public void rollbackEvent(EventDTO event) {
        changeValidationToFail(event);
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executado para validação do produto.");
        producer.sendEvent(jsonUtil.toJson(event));
    }

    public void changeValidationToFail(EventDTO event) {
        validationRepository
                .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getPayload().getTransactionId())
                .ifPresentOrElse(validation -> {
                            validation.setSuccess(false);
                            validationRepository.save(validation);
                        },
                        () -> createValidation(event, false));
    }

}
