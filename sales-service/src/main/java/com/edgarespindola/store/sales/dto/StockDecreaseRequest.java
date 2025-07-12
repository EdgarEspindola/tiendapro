package com.edgarespindola.store.sales.dto;

import lombok.Data;

@Data
public class StockDecreaseRequest {
    private Long productId;
    private Integer quantity;
}
