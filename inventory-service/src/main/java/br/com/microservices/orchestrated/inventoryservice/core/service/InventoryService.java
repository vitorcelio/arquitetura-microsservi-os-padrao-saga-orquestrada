package br.com.microservices.orchestrated.inventoryservice.core.service;

import br.com.microservices.orchestrated.inventoryservice.core.dto.EventDTO;
import br.com.microservices.orchestrated.inventoryservice.core.dto.HistoryDTO;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderDTO;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProductDTO;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ValidateException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus.*;

@Slf4j
@AllArgsConstructor
@Service
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final InventoryRepository repository;
    private final OrderInventoryRepository orderInventoryRepository;

    public void updateInventory(EventDTO event) {
        try {
            checkCurrentValidation(event);
            createOrderInventory(event);
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Erro ao tentar atualizar inventário: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(EventDTO event) {
        if (orderInventoryRepository.existsByOrderIdAndTransactionId(event.getPayload().getId(),
                event.getPayload().getTransactionId())) {
            throw new ValidateException("Já existe uma transação aberta para essa validação");
        }
    }

    private void createOrderInventory(EventDTO event) {
        event.getPayload()
                .getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    var orderInventory = createOrderInventory(event, product, inventory);
                    orderInventoryRepository.save(orderInventory);
                });
    }

    private OrderInventory createOrderInventory(EventDTO event, OrderProductDTO orderProduct, Inventory inventory) {
        return OrderInventory.builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(orderProduct.getQuantity())
                .newQuantity(inventory.getAvailable() - orderProduct.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getPayload().getTransactionId())
                .build();
    }

    private void updateInventory(OrderDTO order) {
        order.getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    checkInventory(inventory.getAvailable(), product.getQuantity());
                    inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
                    repository.save(inventory);
                });
    }

    private void checkInventory(int available, int quantity) {
        if (quantity > available) {
            throw new ValidateException("Produto fora de estoque.");
        }
    }

    private void handleSuccess(EventDTO event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Inventário atualizado com sucesso.");
    }

    private void handleFailCurrentNotExecuted(EventDTO event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Falha ao atualizar inventário: ".concat(message));
    }

    public void rollbackInventory(EventDTO event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);

        try {
            returnInventoryToPreviousValues(event);
            addHistory(event, "Roolback realizado para inventário.");
        } catch (Exception e) {
            addHistory(event, "Rollback não executado para inventário: ".concat(e.getMessage()));
        }

        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void returnInventoryToPreviousValues(EventDTO event) {
        orderInventoryRepository
                .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getPayload().getTransactionId())
                .forEach(orderInventory -> {
                    var inventory = orderInventory.getInventory();
                    inventory.setAvailable(orderInventory.getOldQuantity());
                    repository.save(inventory);
                    log.info("Estoque do pedido {} revertido de {} para {}.", event.getPayload().getId(),
                            orderInventory.getNewQuantity(), inventory.getAvailable());
                });
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

    private Inventory findInventoryByProductCode(String productCode) {
        return repository
                .findByProductCode(productCode)
                .orElseThrow(() -> new ValidateException("Inventário não encontrado pela informação do produto"));
    }

}
