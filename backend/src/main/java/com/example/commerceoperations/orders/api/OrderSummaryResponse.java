package com.example.commerceoperations.orders.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.commerceoperations.orders.domain.OrderStatus;

public record OrderSummaryResponse(Long id, OrderStatus status, LocalDateTime placedAt, BuyerResponse buyer, BigDecimal totalAmount) {
}
