package com.example.commerceoperations.orders.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.commerceoperations.orders.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = { "seller", "buyer", "items", "items.product" })
    List<Order> findBySellerIdOrderByPlacedAtDescIdAsc(Long sellerId);

    @EntityGraph(attributePaths = { "seller", "buyer", "items", "items.product" })
    @Query("select o from Order o where o.id = :id")
    java.util.Optional<Order> findWithItemsById(@Param("id") Long id);
}
