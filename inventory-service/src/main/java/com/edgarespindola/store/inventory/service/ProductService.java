package com.edgarespindola.store.inventory.service;

import java.util.List;

import com.edgarespindola.store.inventory.dto.ProductDto;

public interface ProductService {
    List<ProductDto> getAllProducts();

    ProductDto createProduct(ProductDto dto);

    List<ProductDto> getLowStockProducts(int threshold);

    ProductDto getById(Long id);

    ProductDto update(Long id, ProductDto dto);
    
    void delete(Long id);
}
