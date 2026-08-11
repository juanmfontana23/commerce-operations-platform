package com.example.commerceoperations.orders.application;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.OrderStatus;
import com.example.commerceoperations.orders.infrastructure.OrderRepository;
import com.example.commerceoperations.outbox.application.OrderEventHandler;
import com.example.commerceoperations.outbox.domain.OutboxEvent;
import com.example.commerceoperations.outbox.infrastructure.OutboxEventRepository;
import com.example.commerceoperations.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Map<OrderStatus, List<OrderStatus>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);
        VALID_TRANSITIONS.put(OrderStatus.CREATED, List.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PAID, List.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.SHIPPED, List.of(OrderStatus.DELIVERED));
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, List.of());
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, List.of());
    }

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public List<Order> findSellerOrders(Long sellerId, OrderStatus status, String buyer, LocalDate from, LocalDate to) {
        return orderRepository.findBySellerIdOrderByPlacedAtDescIdAsc(sellerId).stream()
                .filter(order -> status == null || order.getStatus() == status)
                .filter(order -> buyer == null || buyer.isBlank() || order.getBuyer().getName().toLowerCase().contains(buyer.toLowerCase()))
                .filter(order -> from == null || !order.getPlacedAt().toLocalDate().isBefore(from))
                .filter(order -> to == null || !order.getPlacedAt().toLocalDate().isAfter(to))
                .toList();
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    public boolean isValidTransition(OrderStatus from, OrderStatus to) {
        List<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, List.of());
        return allowed.contains(to);
    }

    public List<OrderStatus> getAllowedTransitions(OrderStatus current) {
        return VALID_TRANSITIONS.getOrDefault(current, List.of());
    }

    @Transactional
    public Order transitionStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findWithItemsByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        OrderStatus currentStatus = order.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + currentStatus + " -> " + newStatus
                            + ". Allowed transitions from " + currentStatus + ": "
                            + getAllowedTransitions(currentStatus));
        }
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        outboxEventRepository.save(new OutboxEvent(
                OrderEventHandler.ORDER_STATUS_CHANGED + ":" + savedOrder.getId() + ":" + newStatus,
                OrderEventHandler.ORDER_STATUS_CHANGED,
                "ORDER",
                savedOrder.getId(),
                payload(savedOrder, currentStatus, newStatus),
                java.time.LocalDateTime.now()));
        return savedOrder;
    }

    private String payload(Order order, OrderStatus from, OrderStatus to) {
        try {
            Map<String, Object> values = new HashMap<>();
            values.put("orderId", order.getId());
            values.put("from", from);
            values.put("to", to);
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize order event", exception);
        }
    }
}
