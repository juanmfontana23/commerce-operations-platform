package com.example.commerceoperations.orders.api;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productTitle, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
}
