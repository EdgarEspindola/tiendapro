package com.edgarespindola.store.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockDecreaseRequest {
    private Long productId;
    private Integer quantity;
}
