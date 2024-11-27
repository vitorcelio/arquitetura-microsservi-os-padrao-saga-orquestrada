package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.EventDTO;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ValidateException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaHandler.*;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@AllArgsConstructor
@Component
public class SagaExecutionController {

    public static final String SAGA_LOG_ID = "ORDER_ID: %s | TRANSACTION_ID: %s | EVENT_ID: %s";

    public ETopics getNextTopic(EventDTO event) {
        if (isEmpty(event.getStatus()) || isEmpty(event.getSource())) {
            throw new ValidateException("Source e Status devem ser informados.");
        }

        var topic = findTopicBySourceAndStatus(event);
        logCurrentSaga(event, topic);
        return topic;
    }

    private ETopics findTopicBySourceAndStatus(EventDTO event) {
        return (ETopics) (Arrays.stream(SAGA_HANDLER)
                .filter(row -> isEventSourceAndStatusValid(event, row))
                .map(row -> row[TOPIC_INDEX])
                .findFirst()
                .orElseThrow(() -> new ValidateException("Tópico não encontrado.")));
    }

    private boolean isEventSourceAndStatusValid(EventDTO event, Object[] row) {
        var source = row[EVENT_SOURCE_INDEX];
        var status = row[SAGA_STATUS_INDEX];
        return event.getSource().equals(source) && event.getStatus().equals(status);
    }

    private void logCurrentSaga(EventDTO event, ETopics topic) {
        var sagaId = createSagaId(event);
        var source = event.getSource();
        switch (event.getStatus()) {
            case SUCCESS -> log.info("### CURRENT SAGA: {} | SUCCESS | NEXT TOPIC {} | {}", source, topic, sagaId);
            case ROLLBACK_PENDING ->
                    log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK CURRENT SERVICE | NEXT TOPIC {} | {}",
                            source, topic, sagaId);
            case FAIL ->
                    log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK PREVIOUS SERVICE | NEXT TOPIC {} | {}",
                            source, topic, sagaId);
        }
    }

    private String createSagaId(EventDTO event) {
        return String.format(SAGA_LOG_ID, event.getPayload().getId(),
                event.getPayload().getTransactionId(), event.getId());
    }

}
