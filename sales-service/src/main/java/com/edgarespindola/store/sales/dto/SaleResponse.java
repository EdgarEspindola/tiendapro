package com.edgarespindola.store.sales.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class SaleResponse {
    private Long saleId;
    private LocalDateTime saleDate;
    private List<SaleItemResponse> items;
}
