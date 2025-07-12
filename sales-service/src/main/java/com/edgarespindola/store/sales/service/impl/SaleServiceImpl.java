package com.edgarespindola.store.sales.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edgarespindola.store.sales.dto.ProductInventoryResponse;
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

    /* TODO: 
        Cambiarás el localhost:8081 por el host/puerto real que uses, o incluso por el nombre de servicio si usas Eureka o Docker Compose más adelante.
        Checar FEIGN Client
    */    
    private ProductInventoryResponse getProductFromInventory(Long productId) {
        String url = "http://localhost:8081/api/products/" + productId;
        return restTemplate.getForObject(url, ProductInventoryResponse.class);
    }
}
