package br.com.microservices.orchestrated.productvalidationservice.core.consumer;

import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ProductValidationConsumer {

    private final JsonUtil jsonUtil;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.orchestrator}"
    )
    public void consumerOrchestratorEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico orchestrator", payload);
        var json = jsonUtil.toJson(payload);
        log.info(json);
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.product-validation-success}"
    )
    public void consumerSuccessEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico product-validation-success", payload);
        var json = jsonUtil.toJson(payload);
        log.info(json);
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.product-validation-fail}"
    )
    public void consumerFailEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico product-validation-fail", payload);
        var json = jsonUtil.toJson(payload);
        log.info(json);
    }

}