package com.edgarespindola.store.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edgarespindola.store.inventory.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStockLessThanEqual(int threshold);
}
