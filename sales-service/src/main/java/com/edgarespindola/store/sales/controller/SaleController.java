package com.edgarespindola.store.sales.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edgarespindola.store.sales.dto.SaleRequest;
import com.edgarespindola.store.sales.dto.SaleResponse;
import com.edgarespindola.store.sales.service.SaleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

     private final SaleService saleService;

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@RequestBody SaleRequest request) {
        return ResponseEntity.ok(saleService.createSale(request));
    }

    @GetMapping("/by-product/{productId}")
    public ResponseEntity<List<SaleResponse>> getSalesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(saleService.findByProductId(productId));
    }
}
