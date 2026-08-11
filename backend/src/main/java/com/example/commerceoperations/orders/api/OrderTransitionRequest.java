package com.example.commerceoperations.orders.api;

import com.example.commerceoperations.orders.domain.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderTransitionRequest(@NotNull OrderStatus status) {
}
