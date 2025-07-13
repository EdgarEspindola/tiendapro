package com.edgarespindola.store.sales.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.edgarespindola.store.sales.client.CustomerClient;
import com.edgarespindola.store.sales.client.InventoryClient;
import com.edgarespindola.store.sales.dto.ProductInventoryResponse;
import com.edgarespindola.store.sales.dto.SaleRequest;
import com.edgarespindola.store.sales.dto.SaleResponse;
import com.edgarespindola.store.sales.mapper.SaleMapper;
import com.edgarespindola.store.sales.model.Sale;
import com.edgarespindola.store.sales.model.SaleItem;
import com.edgarespindola.store.sales.repository.SaleRepository;
import com.edgarespindola.store.sales.service.SaleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {
    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;
    private final RestTemplate restTemplate;
    private final CustomerClient customerClient;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public SaleResponse createSale(SaleRequest request) {
        customerClient.validateCustomerExists(request.getCustomerId());

        Sale sale = saleMapper.toEntity(request);
        sale.setSaleDate(LocalDateTime.now());

        for (var item : sale.getItems()) {
            ProductInventoryResponse product = inventoryClient.getProductFromInventory(item.getProductId());

            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("Stock insuficiente para el producto ID: " + item.getProductId());
            }

            item.setUnitPrice(product.getPrice());
            item.setSale(sale);
        }

        BigDecimal total = sale.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sale.setTotalAmount(total);

        Sale saved = saleRepository.save(sale);

        for (SaleItem item : saved.getItems()) {
            inventoryClient.decreaseProductStock(item.getProductId(), item.getQuantity());
        }

        notifyCustomer(sale);
        return saleMapper.toDto(saved);
    }

    @Override
    public List<SaleResponse> findByProductId(Long productId) {
        return saleRepository.findByItemsProductId(productId)
                .stream()
                .map(saleMapper::toDto)
                .collect(Collectors.toList());
    }

    private void notifyCustomer(Sale sale) {
        String url = "http://localhost:8084/api/notifications/sale";

        SaleResponse payload = saleMapper.toDto(sale);

        try {
            restTemplate.postForEntity(url, payload, Void.class);
            log.info("✅ Notificación enviada para cliente {}", sale.getCustomerId());
        } catch (Exception ex) {
            log.error("❌ Error al notificar al cliente", ex);
        }
    }
}
