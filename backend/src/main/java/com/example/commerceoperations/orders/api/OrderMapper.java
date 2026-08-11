package com.example.commerceoperations.orders.api;

import java.util.List;

import com.example.commerceoperations.orders.domain.Buyer;
import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.OrderItem;

final class OrderMapper {

    private OrderMapper() {
    }

    static OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getStatus(), order.getPlacedAt(), toBuyer(order.getBuyer()), order.getTotalAmount());
    }

    static OrderDetailResponse toDetail(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderMapper::toItem)
                .toList();
        return new OrderDetailResponse(
                order.getId(),
                order.getSeller().getId(),
                order.getStatus(),
                order.getPlacedAt(),
                toBuyer(order.getBuyer()),
                items,
                order.getTotalAmount());
    }

    private static BuyerResponse toBuyer(Buyer buyer) {
        return new BuyerResponse(buyer.getId(), buyer.getName(), buyer.getEmail());
    }

    private static OrderItemResponse toItem(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProductTitle(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal());
    }
}
