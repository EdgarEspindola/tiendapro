package com.edgarespindola.store.sales.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SaleItemResponse {
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
