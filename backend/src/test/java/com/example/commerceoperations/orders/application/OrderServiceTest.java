package com.example.commerceoperations.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.commerceoperations.orders.domain.Buyer;
import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.OrderStatus;
import com.example.commerceoperations.orders.domain.Product;
import com.example.commerceoperations.orders.domain.Seller;
import com.example.commerceoperations.orders.infrastructure.OrderRepository;
import com.example.commerceoperations.outbox.infrastructure.OutboxEventRepository;
import com.example.commerceoperations.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OrderService orderService;

    private Seller seller;
    private Buyer buyer1;
    private Buyer buyer2;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, outboxEventRepository, new ObjectMapper());

        seller = new Seller("Test Shop", "shop@test.com");
        buyer1 = new Buyer("Alice Smith", "alice@test.com");
        buyer2 = new Buyer("Bob Johnson", "bob@test.com");
        product1 = new Product(seller, "Widget", "Electronics", new BigDecimal("99.99"));
        product2 = new Product(seller, "Gadget", "Home", new BigDecimal("49.99"));
    }

    private Order createOrder(Buyer buyer, OrderStatus status, LocalDate placedDate) {
        Order order = new Order(seller, buyer, status, placedDate.atStartOfDay());
        order.addItem(product1, 1);
        return order;
    }

    @Test
    void findSellerOrders_returnsAllOrdersWhenNoFilters() {
        Order order1 = createOrder(buyer1, OrderStatus.PAID, LocalDate.of(2026, 1, 15));
        Order order2 = createOrder(buyer2, OrderStatus.SHIPPED, LocalDate.of(2026, 1, 20));

        when(orderRepository.findBySellerIdOrderByPlacedAtDescIdAsc(1L)).thenReturn(List.of(order1, order2));

        List<Order> results = orderService.findSellerOrders(1L, null, null, null, null);

        assertThat(results).hasSize(2);
    }

    @Test
    void findSellerOrders_filterByStatus() {
        Order paid = createOrder(buyer1, OrderStatus.PAID, LocalDate.of(2026, 1, 15));
        Order shipped = createOrder(buyer2, OrderStatus.SHIPPED, LocalDate.of(2026, 1, 20));

        when(orderRepository.findBySellerIdOrderByPlacedAtDescIdAsc(1L)).thenReturn(List.of(paid, shipped));

        List<Order> results = orderService.findSellerOrders(1L, OrderStatus.PAID, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void findSellerOrders_filterByBuyerName() {
        Order order1 = createOrder(buyer1, OrderStatus.PAID, LocalDate.of(2026, 1, 15));
        Order order2 = createOrder(buyer2, OrderStatus.PAID, LocalDate.of(2026, 1, 20));

        when(orderRepository.findBySellerIdOrderByPlacedAtDescIdAsc(1L)).thenReturn(List.of(order1, order2));

        List<Order> results = orderService.findSellerOrders(1L, null, "alice", null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBuyer().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void findSellerOrders_filterByDateRange() {
        Order oldOrder = createOrder(buyer1, OrderStatus.PAID, LocalDate.of(2026, 1, 10));
        Order recentOrder = createOrder(buyer2, OrderStatus.PAID, LocalDate.of(2026, 6, 15));

        when(orderRepository.findBySellerIdOrderByPlacedAtDescIdAsc(1L)).thenReturn(List.of(oldOrder, recentOrder));

        List<Order> results = orderService.findSellerOrders(1L, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlacedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void findSellerOrders_buyerFilterIsCaseInsensitive() {
        Order order = createOrder(buyer1, OrderStatus.PAID, LocalDate.of(2026, 1, 15));

        when(orderRepository.findBySellerIdOrderByPlacedAtDescIdAsc(1L)).thenReturn(List.of(order));

        List<Order> results = orderService.findSellerOrders(1L, null, "ALICE", null, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void findSellerOrders_preservesStableRepositoryOrder() {
        Order newer = createOrder(buyer2, OrderStatus.SHIPPED, LocalDate.of(2026, 1, 20));
        Order older = createOrder(buyer1, OrderStatus.PAID, LocalDate.of(2026, 1, 15));

        when(orderRepository.findBySellerIdOrderByPlacedAtDescIdAsc(1L)).thenReturn(List.of(newer, older));

        assertThat(orderService.findSellerOrders(1L, null, null, null, null))
                .containsExactly(newer, older);
    }

    @Test
    void getOrder_returnsOrderWithItems() {
        Order order = createOrder(buyer1, OrderStatus.PAID, LocalDate.of(2026, 1, 15));
        order.addItem(product2, 2);

        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrder(1L);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("199.97"));
    }

    @Test
    void getOrder_throwsWhenNotFound() {
        when(orderRepository.findWithItemsById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void isValidTransition_CREATED_to_PAID() {
        assertThat(orderService.isValidTransition(OrderStatus.CREATED, OrderStatus.PAID)).isTrue();
    }

    @Test
    void isValidTransition_CREATED_to_CANCELLED() {
        assertThat(orderService.isValidTransition(OrderStatus.CREATED, OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void isValidTransition_DELIVERED_to_CREATED_returnsFalse() {
        assertThat(orderService.isValidTransition(OrderStatus.DELIVERED, OrderStatus.CREATED)).isFalse();
    }

    @Test
    void isValidTransition_CANCELLED_to_PAID_returnsFalse() {
        assertThat(orderService.isValidTransition(OrderStatus.CANCELLED, OrderStatus.PAID)).isFalse();
    }

    @Test
    void isValidTransition_SHIPPED_to_DELIVERED() {
        assertThat(orderService.isValidTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    void isValidTransition_PAID_to_SHIPPED() {
        assertThat(orderService.isValidTransition(OrderStatus.PAID, OrderStatus.SHIPPED)).isTrue();
    }

    @Test
    void transitionStatus_validTransition_succeeds() {
        Order order = createOrder(buyer1, OrderStatus.CREATED, LocalDate.of(2026, 1, 15));

        when(orderRepository.findWithItemsByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.transitionStatus(1L, OrderStatus.PAID);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).findWithItemsByIdForUpdate(1L);
    }

    @Test
    void transitionStatus_invalidTransition_throwsIllegalState() {
        Order order = createOrder(buyer1, OrderStatus.DELIVERED, LocalDate.of(2026, 1, 15));

        when(orderRepository.findWithItemsByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.transitionStatus(1L, OrderStatus.CREATED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
