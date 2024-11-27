package br.com.microservices.orchestrated.inventoryservice.core.dto;

import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventDTO {

    private String id;
    private String transactionId;
    private String orderId;
    private OrderDTO payload;
    private String source;
    private ESagaStatus status;
    private List<HistoryDTO> eventHistory;

    public void addToHistory(HistoryDTO historyDTO) {
        if(isEmpty(this.eventHistory)) {
            this.eventHistory = new ArrayList<>();
        }

        eventHistory.add(historyDTO);
    }

}
