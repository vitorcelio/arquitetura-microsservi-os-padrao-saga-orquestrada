package br.com.microservices.orchestrated.paymentservice.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDTO {

    private String id;
    private List<OrderProductDTO> products;
    private LocalDateTime createdAt;
    private String transactionId;
    private double totalAmount;
    private int totalItems;


}
