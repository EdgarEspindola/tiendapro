package com.edgarespindola.store.sales.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edgarespindola.store.sales.dto.ProductInventoryResponse;
import com.edgarespindola.store.sales.dto.SaleRequest;
import com.edgarespindola.store.sales.dto.SaleResponse;
import com.edgarespindola.store.sales.dto.StockDecreaseRequest;
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

    @Override
    public SaleResponse createSale(SaleRequest request) {
        Sale sale = saleMapper.toEntity(request);
        sale.setSaleDate(LocalDateTime.now());

        for (var item : sale.getItems()) {
            ProductInventoryResponse product = getProductFromInventory(item.getProductId());

            if (product.getStock() < item.getQuantity()) {
                // TODO: Cambiar por excepcion de dominio
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
            // TODO: Que pasa si hay un fallo a la mitad del for y la venta ya se registro
            // en BD
            decreaseProductStock(item.getProductId(), item.getQuantity());
        }
        return saleMapper.toDto(saved);
    }

    @Override
    public List<SaleResponse> findByProductId(Long productId) {
        return saleRepository.findByItemsProductId(productId)
                .stream()
                .map(saleMapper::toDto)
                .collect(Collectors.toList());
    }

    /*
     * TODO:
     * Cambiarás el localhost:8081 por el host/puerto real que uses, o incluso por
     * el nombre de servicio si usas Eureka o Docker Compose más adelante.
     * Checar FEIGN Client
     */
    private ProductInventoryResponse getProductFromInventory(Long productId) {
        String url = "http://localhost:8081/api/products/" + productId;
        return restTemplate.getForObject(url, ProductInventoryResponse.class);
    }

    private void decreaseProductStock(Long productId, Integer quantity) {
        String url = "http://localhost:8081/api/products/" + productId + "/stock/decrease";

        StockDecreaseRequest request = new StockDecreaseRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);

        try {
            restTemplate.put(url, request);
            log.info("✔️ Stock actualizado para producto {}", productId);
        } catch (Exception ex) {
            log.error("❌ No se pudo descontar stock para producto {}", productId, ex);
            throw new IllegalStateException("Error al descontar stock. Venta cancelada.");
        }
    }
}
