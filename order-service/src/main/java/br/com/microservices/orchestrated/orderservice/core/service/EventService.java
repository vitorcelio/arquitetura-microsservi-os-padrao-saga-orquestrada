package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class EventService {

    private final EventRepository repository;

    public void notifyEnding(Event event) {
        event.setOrderId(event.getOrderId());
        event.setCreatedAt(LocalDateTime.now());
        save(event);
        log.info("Pedido {} com saga notificada! TransactionId {}", event.getOrderId(), event.getTransactionId());
    }

    public Event save(Event event) {
        return repository.save(event);
    }

    public List<Event> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Event findByFilters(EventFilters filters) {
        if(filters.isInvalid()) {
            throw new ValidationException("OrderID ou Transaction ID inválido");
        }

        if(!StringUtils.isEmpty(filters.getOrderId())) {
            return findByOrderId(filters.getOrderId());
        } else {
            return findByTransactionId(filters.getTransactionId());
        }

    }

    private Event findByOrderId(String orderId) {
        return repository
                .findTop1ByOrderIdOrderByCreatedAt(orderId)
                .orElseThrow(() -> new ValidationException("Event not found OrderId"));
    }

    private Event findByTransactionId(String transactionId) {
        return repository
                .findTop1ByTransactionIdOrderByCreatedAt(transactionId)
                .orElseThrow(() -> new ValidationException("Event not found transactionId"));
    }

}
