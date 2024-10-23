package br.com.microservices.orchestrated.inventoryservice.core.utils;

import br.com.microservices.orchestrated.inventoryservice.core.dto.EventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JsonUtil {

    private final ObjectMapper objectMapper;

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return "";
        }
    }

    public EventDTO toEvent(String object) {
        try {
            return objectMapper.readValue(object, EventDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

}
