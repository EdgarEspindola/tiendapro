package com.edgarespindola.store.sales.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.edgarespindola.store.sales.dto.ProductInventoryResponse;
import com.edgarespindola.store.sales.dto.StockDecreaseRequest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {
    private final RestTemplate restTemplate;

    private static final String CUSTOMER_BASE_URL = "http://localhost:8081/api/products/";

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackGetProduct")
    @Retry(name = "inventoryService")
    public ProductInventoryResponse getProductFromInventory(Long productId) {
        String url = CUSTOMER_BASE_URL + productId;
        try {
            return restTemplate.getForObject(url, ProductInventoryResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Producto no encontrado con ID: " + productId);
        }
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackDecreaseStock")
    @Retry(name = "inventoryService")
    public void decreaseProductStock(Long productId, Integer quantity) {
        String url = CUSTOMER_BASE_URL + productId + "/stock/decrease";
        StockDecreaseRequest request = new StockDecreaseRequest(productId, quantity);
        try {
            restTemplate.put(url, request);
            log.info("Stock actualizado para producto {}", productId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException(
                    "No se pudo descontar stock. Producto no encontrado con ID: " + productId);
        }
    }

    private ProductInventoryResponse fallbackGetProduct(Long productId, Throwable ex) {
        log.warn("⚠️ Fallback activado para producto ID {}. Razón: {}", productId, ex.getMessage());
        throw new IllegalStateException(
                "No se pudo obtener el producto " + productId + ". Inventory temporalmente fuera de servicio.");
    }

    private void fallbackDecreaseStock(Long productId, Integer quantity, Throwable ex) {
        log.error("❌ Fallback al descontar stock para producto {}: {}", productId, ex.getMessage());
        throw new IllegalStateException("No se pudo descontar stock. Servicio de inventario no disponible.");
    }

}
