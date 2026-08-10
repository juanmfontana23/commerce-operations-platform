package com.example.commerceoperations.orders.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.example.commerceoperations.orders.domain.Seller;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByEmail(String email);
}
