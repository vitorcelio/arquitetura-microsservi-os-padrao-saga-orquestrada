package br.com.microservices.orchestrated.productvalidationservice.core.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.orchestrator}")
    private String orchestratorTopic;

    public void sendEvent(String payload) {
        try {
            log.info("Enviando evento para tópico {} com os dados {}", orchestratorTopic, payload);
            kafkaTemplate.send(orchestratorTopic, payload);
            log.info("Finalizado Envio de evento para o tópico {} com os dados {}", orchestratorTopic, payload);
        } catch (Exception e) {
            log.error("Erro ao tentar enviar dados para tópico {} com os dados {}", orchestratorTopic, payload, e);
        }
    }

}
