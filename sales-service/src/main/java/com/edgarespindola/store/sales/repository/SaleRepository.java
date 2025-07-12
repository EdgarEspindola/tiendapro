package com.edgarespindola.store.sales.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edgarespindola.store.sales.model.Sale;

public interface SaleRepository extends JpaRepository<Sale, Long> {
        List<Sale> findByItemsProductId(Long productId);
}
