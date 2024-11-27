package br.com.microservices.orchestrated.orchestratorservice.core.service;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.EventDTO;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.HistoryDTO;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import br.com.microservices.orchestrated.orchestratorservice.core.producer.SagaOrchestratorProducer;
import br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaExecutionController;
import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource.ORCHESTRATOR;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.FAIL;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.SUCCESS;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.NOTIFY_ENDING;

@Slf4j
@AllArgsConstructor
@Service
public class OrchestratorService {

    private final JsonUtil jsonUtil;
    private final SagaOrchestratorProducer producer;
    private final SagaExecutionController sagaExecutionController;

    public void startSaga(EventDTO event) {
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCCESS);
        log.info("SAGA STARTED!");
        addHistory(event, "Iniciando Saga!");
        sendToProducer(event);
    }

    public void finishSagaSuccess(EventDTO event) {
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCCESS);
        log.info("SAGA FINISHED SUCCESSFULLY FOR EVENT {}!", event.getId());
        addHistory(event, "Saga finalizada com sucesso!");
        sendNotifyFinishedSaga(event);
    }

    public void finishSagaFail(EventDTO event) {
        event.setSource(ORCHESTRATOR);
        event.setStatus(FAIL);
        log.info("SAGA FINISHED WITH ERRORS FOR EVENT {}!", event.getId());
        addHistory(event, "Saga finalizada com erros!");
        sendNotifyFinishedSaga(event);
    }

    public void continueSaga(EventDTO event) {
        log.info("SAGA CONTINUE FOR EVENT {}!", event.getId());
        sendToProducer(event);
    }

    private ETopics getTopic(EventDTO event) {
        return sagaExecutionController.getNextTopic(event);
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

    private void sendToProducer(EventDTO event) {
        var topic = getTopic(event);
        producer.sendEvent(topic.getTopic(), jsonUtil.toJson(event));
    }

    private void sendNotifyFinishedSaga(EventDTO event) {
        producer.sendEvent(NOTIFY_ENDING.getTopic(), jsonUtil.toJson(event));
    }

}
