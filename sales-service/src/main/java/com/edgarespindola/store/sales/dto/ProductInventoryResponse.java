package com.edgarespindola.store.sales.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductInventoryResponse {
    private Long id;
    private String name;
    private Integer stock;
    private BigDecimal price;
}
