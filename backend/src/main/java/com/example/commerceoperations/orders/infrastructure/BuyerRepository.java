package com.example.commerceoperations.orders.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.commerceoperations.orders.domain.Buyer;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
}
