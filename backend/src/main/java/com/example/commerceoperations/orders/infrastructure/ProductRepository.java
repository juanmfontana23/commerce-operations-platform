package com.example.commerceoperations.orders.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.commerceoperations.orders.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
