package br.com.microservices.orchestrated.orchestratorservice.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDTO {

    private String id;
    private List<OrderProductDTO> products;
    private LocalDate createdAt;
    private String transactionId;
    private double totalAmount;
    private int totalItems;


}
