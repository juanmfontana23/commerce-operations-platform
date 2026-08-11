package com.example.commerceoperations.shared.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.example.commerceoperations.orders.domain.Buyer;
import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.OrderStatus;
import com.example.commerceoperations.orders.domain.Product;
import com.example.commerceoperations.orders.domain.Seller;
import com.example.commerceoperations.orders.infrastructure.BuyerRepository;
import com.example.commerceoperations.orders.infrastructure.OrderRepository;
import com.example.commerceoperations.orders.infrastructure.ProductRepository;
import com.example.commerceoperations.orders.infrastructure.SellerRepository;
import com.example.commerceoperations.questions.application.QuestionService;
import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.questions.infrastructure.QuestionRepository;

@Configuration
@Profile({ "local", "test" })
public class DataSeedConfig {

    @Bean
    CommandLineRunner seedData(
            SellerRepository sellerRepository,
            BuyerRepository buyerRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            QuestionRepository questionRepository,
        QuestionService questionService) {
        return args -> {
            if (sellerRepository.findByEmail("seller@example.com").isPresent()) {
                return;
            }
            Seller seller = sellerRepository.save(new Seller("Electro Shop BA", "seller@example.com"));

            // --- 6 Buyers ---
            Buyer sofia = buyerRepository.save(new Buyer("Sofia Alvarez", "sofia.alvarez@example.com"));
            Buyer martin = buyerRepository.save(new Buyer("Martin Perez", "martin.perez@example.com"));
            Buyer camila = buyerRepository.save(new Buyer("Camila Torres", "camila.torres@example.com"));
            Buyer lucas = buyerRepository.save(new Buyer("Lucas Rodriguez", "lucas.rodriguez@example.com"));
            Buyer valentina = buyerRepository.save(new Buyer("Valentina Lopez", "valentina.lopez@example.com"));
            Buyer diego = buyerRepository.save(new Buyer("Diego Fernandez", "diego.fernandez@example.com"));

            // --- 8 Products ---
            Product headphones = productRepository.save(new Product(seller, "Noise Cancelling Headphones", "Electronics", new BigDecimal("249.99")));
            Product coffeeMaker = productRepository.save(new Product(seller, "Smart Coffee Maker", "Home", new BigDecimal("139.90")));
            Product laptopStand = productRepository.save(new Product(seller, "Aluminum Laptop Stand", "Office", new BigDecimal("79.50")));
            Product camera = productRepository.save(new Product(seller, "4K Action Camera", "Electronics", new BigDecimal("329.00")));
            Product blender = productRepository.save(new Product(seller, "Professional Blender", "Home", new BigDecimal("199.00")));
            Product monitor = productRepository.save(new Product(seller, "27in 4K Monitor", "Electronics", new BigDecimal("549.99")));
            Product desk = productRepository.save(new Product(seller, "Standing Desk Converter", "Office", new BigDecimal("275.00")));
            Product speaker = productRepository.save(new Product(seller, "Portable Bluetooth Speaker", "Electronics", new BigDecimal("89.99")));

            // --- 6 Orders ---
            Order paidOrder = new Order(seller, sofia, OrderStatus.PAID, LocalDateTime.now().minusDays(1));
            paidOrder.addItem(headphones, 1);
            paidOrder.addItem(laptopStand, 2);

            Order shippedOrder = new Order(seller, martin, OrderStatus.SHIPPED, LocalDateTime.now().minusDays(3));
            shippedOrder.addItem(camera, 1);
            shippedOrder.addItem(coffeeMaker, 1);

            Order deliveredOrder = new Order(seller, camila, OrderStatus.DELIVERED, LocalDateTime.now().minusDays(8));
            deliveredOrder.addItem(coffeeMaker, 1);

            Order cancelledOrder = new Order(seller, sofia, OrderStatus.CANCELLED, LocalDateTime.now().minusDays(2));
            cancelledOrder.addItem(laptopStand, 1);

            Order createdOrder = new Order(seller, lucas, OrderStatus.CREATED, LocalDateTime.now().minusHours(6));
            createdOrder.addItem(monitor, 1);
            createdOrder.addItem(desk, 1);

            Order shippedOrder2 = new Order(seller, valentina, OrderStatus.SHIPPED, LocalDateTime.now().minusDays(5));
            shippedOrder2.addItem(blender, 1);
            shippedOrder2.addItem(speaker, 2);

            orderRepository.saveAll(List.of(paidOrder, shippedOrder, deliveredOrder, cancelledOrder, createdOrder, shippedOrder2));

            // --- 8 Questions ---
            Question urgentDelivery = new Question(
                    shippedOrder,
                    martin,
                    camera,
                    "Urgent: the package was not received and I need it for a trip.",
                    LocalDateTime.now().minusHours(55));
            Question warrantyQuestion = new Question(
                    paidOrder,
                    sofia,
                    headphones,
                    "Does this product include warranty and invoice?",
                    LocalDateTime.now().minusHours(18));
            Question setupQuestion = new Question(
                    deliveredOrder,
                    camila,
                    coffeeMaker,
                    "Can you send setup instructions for the smart features?",
                    LocalDateTime.now().minusHours(4));
            Question refundQuestion = new Question(
                    cancelledOrder,
                    sofia,
                    laptopStand,
                    "I cancelled the order and need a refund confirmation.",
                    LocalDateTime.now().minusHours(30));
            Question brokenProduct = new Question(
                    shippedOrder2,
                    valentina,
                    blender,
                    "The blender arrived broken and leaking. I need an urgent replacement.",
                    LocalDateTime.now().minusHours(72));
            Question oldQuestion = new Question(
                    deliveredOrder,
                    camila,
                    null,
                    "General inquiry about return policy for non-defective items.",
                    LocalDateTime.now().minusDays(15));
            Question quickQuestion = new Question(
                    createdOrder,
                    lucas,
                    monitor,
                    "What is the refresh rate of this monitor?",
                    LocalDateTime.now().minusHours(2));
            Question cancelRequest = new Question(
                    createdOrder,
                    lucas,
                    desk,
                    "Cancel the order please, I found a better price.",
                    LocalDateTime.now().minusHours(10));

            // ANSWERED question
            setupQuestion.answer("The setup guide is available in the product box and online manual.", LocalDateTime.now().minusHours(2));

            // RESOLVED question
            refundQuestion.answer("Refund has been processed. Allow 3-5 business days.", LocalDateTime.now().minusHours(20));
            refundQuestion.resolve(LocalDateTime.now().minusHours(10));

            List<Question> questions = List.of(
                    urgentDelivery, warrantyQuestion, setupQuestion, refundQuestion,
                    brokenProduct, oldQuestion, quickQuestion, cancelRequest);

            questionRepository.saveAll(questions)
                    .forEach(questionService::refreshPriorityAndNotify);
        };
    }
}
