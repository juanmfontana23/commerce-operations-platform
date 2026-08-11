package com.example.commerceoperations.shared.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.commerceoperations.orders.domain.Seller;
import com.example.commerceoperations.orders.infrastructure.BuyerRepository;
import com.example.commerceoperations.orders.infrastructure.OrderRepository;
import com.example.commerceoperations.orders.infrastructure.ProductRepository;
import com.example.commerceoperations.orders.infrastructure.SellerRepository;
import com.example.commerceoperations.questions.application.QuestionService;
import com.example.commerceoperations.questions.infrastructure.QuestionRepository;

class DataSeedConfigTest {

    @Test
    void seedData_skipsWhenDemoSellerAlreadyExists() throws Exception {
        SellerRepository sellerRepository = mock(SellerRepository.class);
        BuyerRepository buyerRepository = mock(BuyerRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        QuestionService questionService = mock(QuestionService.class);
        when(sellerRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(new Seller("Electro Shop BA", "seller@example.com")));

        new DataSeedConfig().seedData(sellerRepository, buyerRepository, productRepository,
                orderRepository, questionRepository, questionService).run();

        verifyNoInteractions(buyerRepository, productRepository, orderRepository, questionRepository, questionService);
    }
}
