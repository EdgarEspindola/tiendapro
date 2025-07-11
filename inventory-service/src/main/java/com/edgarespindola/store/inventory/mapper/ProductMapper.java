package com.edgarespindola.store.inventory.mapper;

import com.edgarespindola.store.inventory.dto.ProductDto;
import com.edgarespindola.store.inventory.entity.Product;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDto toDto(Product product);
    Product toEntity(ProductDto dto);
}
