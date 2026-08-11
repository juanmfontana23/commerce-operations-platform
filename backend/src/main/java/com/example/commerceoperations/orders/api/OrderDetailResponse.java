package com.example.commerceoperations.orders.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.commerceoperations.orders.domain.OrderStatus;

public record OrderDetailResponse(
        Long id,
        Long sellerId,
        OrderStatus status,
        LocalDateTime placedAt,
        BuyerResponse buyer,
        List<OrderItemResponse> items,
        BigDecimal totalAmount) {
}
