package com.example.commerceoperations.orders.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.example.commerceoperations.orders.application.OrderService;
import com.example.commerceoperations.orders.domain.OrderStatus;
import com.example.commerceoperations.shared.security.SellerAuthorization;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final SellerAuthorization sellerAuthorization;

    public OrderController(OrderService orderService, SellerAuthorization sellerAuthorization) {
        this.orderService = orderService;
        this.sellerAuthorization = sellerAuthorization;
    }

    @GetMapping("/sellers/{sellerId}/orders")
    List<OrderSummaryResponse> findSellerOrders(
            @PathVariable Long sellerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String buyer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        sellerAuthorization.requireSeller(authentication, sellerId);
        return orderService.findSellerOrders(sellerId, status, buyer, from, to).stream()
                .map(OrderMapper::toSummary)
                .toList();
    }

    @GetMapping("/orders/{orderId}")
    OrderDetailResponse getOrder(@PathVariable Long orderId, Authentication authentication) {
        var order = orderService.getOrder(orderId);
        sellerAuthorization.requireOrder(authentication, order);
        return OrderMapper.toDetail(order);
    }

    @PostMapping("/orders/{orderId}/transition")
    OrderDetailResponse transitionOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderTransitionRequest request,
            Authentication authentication) {
        sellerAuthorization.requireOrder(authentication, orderService.getOrder(orderId));
        return OrderMapper.toDetail(orderService.transitionStatus(orderId, request.status()));
    }
}
