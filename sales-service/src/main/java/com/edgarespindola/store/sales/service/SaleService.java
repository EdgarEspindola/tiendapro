package com.edgarespindola.store.sales.service;

import java.util.List;

import com.edgarespindola.store.sales.dto.SaleRequest;
import com.edgarespindola.store.sales.dto.SaleResponse;

public interface SaleService {
    SaleResponse createSale(SaleRequest request);
    List<SaleResponse> findByProductId(Long productId);
}
