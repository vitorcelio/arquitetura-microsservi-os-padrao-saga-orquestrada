package br.com.microservices.orchestrated.orchestratorservice.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDTO {

    private ProductDTO product;
    private int quantity;

}
