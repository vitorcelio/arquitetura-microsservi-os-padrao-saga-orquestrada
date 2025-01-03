package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.document.Order;
import br.com.microservices.orchestrated.orderservice.core.dto.OrderRequest;
import br.com.microservices.orchestrated.orderservice.core.producer.SagaProducer;
import br.com.microservices.orchestrated.orderservice.core.repository.OrderRepository;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private static final String TRANSACTIONAL_ID_PATTERN = "%s_%s";

    private EventService eventService;
    private final JsonUtil jsonUtil;
    private final SagaProducer sagaProducer;
    private final OrderRepository repository;

    public Order createOrder(OrderRequest request) {
        var order = Order.builder()
                .products(request.getProducts())
                .createdAt(LocalDateTime.now())
                .transactionId(
                        String.format(TRANSACTIONAL_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID())
                )
                .build();

        repository.save(order);
        String json = jsonUtil.toJson(createPayload(order));
        sagaProducer.sendEvent(json);
        return order;
    }

    private Event createPayload(Order order) {
        var event = Event.builder()
                .orderId(order.getId())
                .payload(order)
                .transactionId(order.getTransactionId())
                .createdAt(LocalDateTime.now())
                .build();

        return eventService.save(event);
    }

}
