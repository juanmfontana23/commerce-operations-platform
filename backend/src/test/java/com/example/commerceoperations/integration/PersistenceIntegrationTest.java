package com.example.commerceoperations.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.commerceoperations.orders.application.OrderService;
import com.example.commerceoperations.orders.domain.OrderStatus;
import com.example.commerceoperations.orders.domain.Seller;
import com.example.commerceoperations.outbox.domain.OutboxEventStatus;
import com.example.commerceoperations.outbox.infrastructure.OutboxEventRepository;
import com.example.commerceoperations.orders.infrastructure.OrderRepository;
import com.example.commerceoperations.orders.infrastructure.SellerRepository;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderService orderService;

    @Test
    void flywayCreatesVersionedSchema() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '1' and \"success\" = true",
                Integer.class);

        assertThat(migrationCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'SELLER_ORDERS'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void repositoryPersistsAgainstMigratedSchema() {
        Seller seller = sellerRepository.saveAndFlush(new Seller("Persistence Test Seller", "persistence-test@example.com"));

        assertThat(sellerRepository.findById(seller.getId()))
                .get()
                .extracting(Seller::getEmail)
                .isEqualTo("persistence-test@example.com");
    }

    @Test
    @Transactional
    void orderTransitionPersistsOutboxEventWithOrderChange() {
        var order = orderRepository.findAll().stream()
                .filter(candidate -> candidate.getStatus() == OrderStatus.PAID)
                .findFirst().orElseThrow();

        orderService.transitionStatus(order.getId(), OrderStatus.SHIPPED);

        assertThat(outboxEventRepository.findAll()).anySatisfy(event -> {
            assertThat(event.getAggregateId()).isEqualTo(order.getId());
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(event.getPayload()).contains("SHIPPED");
        });
    }
}
