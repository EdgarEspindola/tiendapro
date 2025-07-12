package com.edgarespindola.store.sales.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.edgarespindola.store.sales.dto.SaleRequest;
import com.edgarespindola.store.sales.dto.SaleResponse;
import com.edgarespindola.store.sales.model.Sale;

@Mapper(componentModel = "spring")
public interface SaleMapper {
    Sale toEntity(SaleRequest dto);

    @Mapping(target = "saleId", source = "id")
    SaleResponse toDto(Sale sale);
}
