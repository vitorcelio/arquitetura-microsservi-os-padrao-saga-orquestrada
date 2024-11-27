package br.com.microservices.orchestrated.orderservice.core.producer;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SagaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonUtil jsonUtil;

    @Value("${spring.kafka.topic.start-saga}")
    private String startSagaTopic;

    public void sendEvent(String payload) {
        try {

            Event event = jsonUtil.toEvent(payload);

            log.info("Enviando evento para tópico {} com os dados {}", startSagaTopic, payload);
            kafkaTemplate.send(startSagaTopic, payload);
            log.info("Finalizado Envio de evento para o tópico {} com os dados {}", startSagaTopic, payload);
        } catch (Exception e) {
            log.error("Erro ao tentar enviar dados para tópico {} com os dados {}", startSagaTopic, payload, e);
        }
    }

}
