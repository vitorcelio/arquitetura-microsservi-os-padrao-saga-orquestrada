package br.com.microservices.orchestrated.orderservice.core.dto;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventFilters {

    private String orderId;
    private String transactionId;

    @Hidden
    public boolean isInvalid() {
        return StringUtils.isEmpty(orderId) && StringUtils.isEmpty(transactionId);
    }

}
