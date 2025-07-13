package com.edgarespindola.store.sales.dto;

import java.util.List;

import lombok.Data;

@Data
public class SaleRequest {
    private Long customerId;
    private List<SaleItemRequest> items;
}
