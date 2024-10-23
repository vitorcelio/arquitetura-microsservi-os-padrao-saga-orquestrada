package br.com.microservices.orchestrated.orchestratorservice.core.consumer;

import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class SagaOrchestratorConsumer {

    private final JsonUtil jsonUtil;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.start-saga}"
    )
    public void consumerStartSagaEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico start-saga", payload);
        var json = jsonUtil.toJson(payload);
        log.info(json);
    }

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
            topics = "${spring.kafka.topic.finish-success}"
    )
    public void consumerFinishSuccessEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico finish-success", payload);
        var json = jsonUtil.toJson(payload);
        log.info(json);
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.finish-fail}"
    )
    public void consumerFinishFailEvent(String payload) {
        log.info("Recebendo evento {} de notificação do tópico finish-fail", payload);
        var json = jsonUtil.toJson(payload);
        log.info(json);
    }

}
