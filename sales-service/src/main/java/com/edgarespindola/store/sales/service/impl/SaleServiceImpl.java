package com.edgarespindola.store.sales.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.edgarespindola.store.sales.dto.SaleRequest;
import com.edgarespindola.store.sales.dto.SaleResponse;
import com.edgarespindola.store.sales.mapper.SaleMapper;
import com.edgarespindola.store.sales.model.Sale;
import com.edgarespindola.store.sales.repository.SaleRepository;
import com.edgarespindola.store.sales.service.SaleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {
    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;

    @Override
    public SaleResponse createSale(SaleRequest request) {
        Sale sale = saleMapper.toEntity(request);
        sale.setSaleDate(LocalDateTime.now());
        sale.getItems().forEach(item -> item.setSale(sale));
        Sale saved = saleRepository.save(sale);
        return saleMapper.toDto(saved);
    }

    @Override
    public List<SaleResponse> findByProductId(Long productId) {
        return saleRepository.findByItemsProductId(productId)
                .stream()
                .map(saleMapper::toDto)
                .collect(Collectors.toList());
    }
}
