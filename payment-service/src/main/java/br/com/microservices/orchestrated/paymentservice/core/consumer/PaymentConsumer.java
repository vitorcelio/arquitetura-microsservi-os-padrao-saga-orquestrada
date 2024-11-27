package br.com.microservices.orchestrated.paymentservice.core.consumer;

import br.com.microservices.orchestrated.paymentservice.core.service.PaymentService;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class PaymentConsumer {

    private final JsonUtil jsonUtil;
    private final PaymentService service;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.payment-success}"
    )
    public void consumerSuccessEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico payment-success", payload);
        var event = jsonUtil.toEvent(payload);
        service.realizePayment(event);
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.payment-fail}"
    )
    public void consumerFailEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico payment-fail", payload);
        var event = jsonUtil.toEvent(payload);
        service.realizeRefund(event);
    }

}
