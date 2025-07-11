package com.edgarespindola.store.inventory.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.edgarespindola.store.inventory.dto.ProductDto;
import com.edgarespindola.store.inventory.entity.Product;
import com.edgarespindola.store.inventory.exception.InvalidProductDataException;
import com.edgarespindola.store.inventory.exception.ProductNotFoundException;
import com.edgarespindola.store.inventory.repository.ProductRepository;
import com.edgarespindola.store.inventory.service.ProductService;

import lombok.extern.slf4j.Slf4j;

import com.edgarespindola.store.inventory.mapper.ProductMapper;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repository;
    private final ProductMapper mapper;

    public ProductServiceImpl(ProductRepository repository, ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<ProductDto> getAllProducts() {
        log.info("Obteniendo todos los productos");
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDto createProduct(ProductDto dto) {
        log.info("Creando producto: {}", dto);
        validate(dto);
        Product product = mapper.toEntity(dto);
        Product saved = repository.save(product);
        log.debug("Producto guardado con ID: {}", saved.getId());
        return mapper.toDto(saved);

    }

    @Override
    public List<ProductDto> getLowStockProducts(int threshold) {
        log.info("Buscando productos con stock <= {}", threshold);
        return repository.findByStockLessThanEqual(threshold)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDto getById(Long id) {
        log.info("Buscando producto por ID: {}", id);
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado: {}", id);
                    return new ProductNotFoundException(id);
                });
    }

    @Override
    public ProductDto update(Long id, ProductDto dto) {
        log.info("Actualizando producto con ID: {}", id);
        validate(dto);
        Product existing = repository.findById(id)
                .orElseThrow(() -> {
                     log.warn("Producto no encontrado para actualizar: {}", id);
                    return new ProductNotFoundException(id);
                });

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setStock(dto.getStock());
        existing.setUnit(dto.getUnit());

        Product updated = repository.save(existing);
        log.debug("Producto actualizado: {}", updated);
        return mapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        log.info("Eliminando producto con ID: {}", id);
        Product product = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado para eliminar: {}", id);
                    return new ProductNotFoundException(id);
                });
        repository.delete(product);
        log.debug("Producto eliminado exitosamente: {}", id);
    }

    private void validate(ProductDto dto) {
        if (dto.getPrice() <= 0) {
            throw new InvalidProductDataException("El precio debe ser mayor a 0");
        }
        if (dto.getStock() < 0) {
            throw new InvalidProductDataException("El stock no puede ser negativo");
        }
    }
}
