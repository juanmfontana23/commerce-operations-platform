package com.example.commerceoperations.questions.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.commerceoperations.orders.domain.Buyer;
import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.OrderStatus;
import com.example.commerceoperations.orders.domain.Product;
import com.example.commerceoperations.orders.domain.Seller;
import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.questions.domain.QuestionPriority;

class QuestionPriorityCalculatorTest {

    private QuestionPriorityCalculator calculator;

    private Seller seller;
    private Buyer buyer;

    @BeforeEach
    void setUp() {
        calculator = new QuestionPriorityCalculator();
        seller = new Seller("Test Seller", "seller@test.com");
        buyer = new Buyer("Test Buyer", "buyer@test.com");
    }

    @Test
    void lowScore_whenRecentCheapOrderNoKeywords() {
        Product product = new Product(seller, "Cheap Shirt", "Clothing", new BigDecimal("25.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusHours(1));
        order.addItem(product, 1);

        Question question = new Question(order, buyer, product, "Where is my order?", LocalDateTime.now().minusHours(2));

        PriorityResult result = calculator.calculate(question, LocalDateTime.now());

        assertThat(result.score()).isEqualTo(5);
        assertThat(result.priority()).isEqualTo(QuestionPriority.LOW);
        assertThat(result.reasons()).contains("Recently created");
    }

    @Test
    void highScore_whenOldExpensiveOrderUrgentKeywordsElectronics() {
        Product product = new Product(seller, "Laptop", "Electronics", new BigDecimal("1200.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusDays(10));
        order.addItem(product, 1);

        Question question = new Question(order, buyer, product,
                "Urgent: the laptop is broken and I need a refund!",
                LocalDateTime.now().minusHours(60));

        PriorityResult result = calculator.calculate(question, LocalDateTime.now());

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.priority()).isEqualTo(QuestionPriority.CRITICAL);
        assertThat(result.reasons()).containsExactlyInAnyOrder(
                "Waiting more than 48 hours",
                "High-value order",
                "Message contains urgent support keywords",
                "Electronics product category");
    }

    @Test
    void scoreBand_LOW_whenScoreBelow40() {
        Product product = new Product(seller, "Pen", "Office", new BigDecimal("5.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusHours(1));
        order.addItem(product, 1);

        Question question = new Question(order, buyer, product, "Hello", LocalDateTime.now().minusHours(2));

        PriorityResult result = calculator.calculate(question, LocalDateTime.now());

        assertThat(result.priority()).isEqualTo(QuestionPriority.LOW);
        assertThat(result.score()).isLessThan(40);
    }

    @Test
    void scoreBand_MEDIUM_whenScoreBetween40And69() {
        Product product = new Product(seller, "Coffee Maker", "Home", new BigDecimal("150.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusDays(10));
        order.addItem(product, 1);

        Question question = new Question(order, buyer, product, "Hello", LocalDateTime.now().minusHours(30));

        PriorityResult result = calculator.calculate(question, LocalDateTime.now());

        assertThat(result.priority()).isEqualTo(QuestionPriority.MEDIUM);
        assertThat(result.score()).isGreaterThanOrEqualTo(40).isLessThan(70);
    }

    @Test
    void scoreBand_HIGH_whenScoreBetween70And89() {
        Product product = new Product(seller, "Laptop", "Electronics", new BigDecimal("600.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusDays(10));
        order.addItem(product, 1);

        Question question = new Question(order, buyer, product, "warranty", LocalDateTime.now().minusHours(50));

        PriorityResult result = calculator.calculate(question, LocalDateTime.now());

        // 35 (waiting) + 25 (high-value) + 10 (administrative keyword) + 15 (electronics) = 85
        assertThat(result.priority()).isEqualTo(QuestionPriority.HIGH);
        assertThat(result.score()).isGreaterThanOrEqualTo(70).isLessThan(90);
    }

    @Test
    void scoreBand_CRITICAL_whenScoreAtLeast90() {
        Product product = new Product(seller, "4K Camera", "Electronics", new BigDecimal("800.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusDays(10));
        order.addItem(product, 1);

        Question question = new Question(order, buyer, product,
                "Urgent: product is broken and not received",
                LocalDateTime.now().minusHours(55));

        PriorityResult result = calculator.calculate(question, LocalDateTime.now());

        assertThat(result.priority()).isEqualTo(QuestionPriority.CRITICAL);
        assertThat(result.score()).isGreaterThanOrEqualTo(90);
    }

    @Test
    void waitingTimeScore_returnsCorrectBands() {
        Product product = new Product(seller, "Item", "Misc", new BigDecimal("10.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusDays(30));
        order.addItem(product, 1);

        // Less than 8 hours => 5
        Question recent = new Question(order, buyer, product, "Hi", LocalDateTime.now().minusHours(4));
        PriorityResult r1 = calculator.calculate(recent, LocalDateTime.now());
        assertThat(r1.reasons()).contains("Recently created");

        // 8-24 hours => 15
        Question medium = new Question(order, buyer, product, "Hi", LocalDateTime.now().minusHours(10));
        PriorityResult r2 = calculator.calculate(medium, LocalDateTime.now());
        assertThat(r2.reasons()).contains("Waiting more than 8 hours");

        // 24-48 hours => 25
        Question older = new Question(order, buyer, product, "Hi", LocalDateTime.now().minusHours(30));
        PriorityResult r3 = calculator.calculate(older, LocalDateTime.now());
        assertThat(r3.reasons()).contains("Waiting more than 24 hours");

        // 48+ hours => 35
        Question oldest = new Question(order, buyer, product, "Hi", LocalDateTime.now().minusHours(50));
        PriorityResult r4 = calculator.calculate(oldest, LocalDateTime.now());
        assertThat(r4.reasons()).contains("Waiting more than 48 hours");
    }

    @Test
    void orderValueScore_returnsCorrectBands() {
        Seller s = seller;
        // Low-value (< 100)
        Product cheap = new Product(s, "Cheap", "Misc", new BigDecimal("50.00"));
        Order cheapOrder = new Order(s, buyer, OrderStatus.PAID, LocalDateTime.now());
        cheapOrder.addItem(cheap, 1);
        Question q1 = new Question(cheapOrder, buyer, cheap, "Hi", LocalDateTime.now());
        assertThat(calculator.calculate(q1, LocalDateTime.now()).score()).isEqualTo(5);

        // Standard (100-249)
        Product standard = new Product(s, "Standard", "Misc", new BigDecimal("150.00"));
        Order stdOrder = new Order(s, buyer, OrderStatus.PAID, LocalDateTime.now());
        stdOrder.addItem(standard, 1);
        Question q2 = new Question(stdOrder, buyer, standard, "Hi", LocalDateTime.now());
        assertThat(calculator.calculate(q2, LocalDateTime.now()).score()).isEqualTo(13);

        // Medium (250-499)
        Product medium = new Product(s, "Medium", "Misc", new BigDecimal("300.00"));
        Order medOrder = new Order(s, buyer, OrderStatus.PAID, LocalDateTime.now());
        medOrder.addItem(medium, 1);
        Question q3 = new Question(medOrder, buyer, medium, "Hi", LocalDateTime.now());
        assertThat(calculator.calculate(q3, LocalDateTime.now()).score()).isEqualTo(20);

        // High (>= 500)
        Product expensive = new Product(s, "Expensive", "Misc", new BigDecimal("600.00"));
        Order expOrder = new Order(s, buyer, OrderStatus.PAID, LocalDateTime.now());
        expOrder.addItem(expensive, 1);
        Question q4 = new Question(expOrder, buyer, expensive, "Hi", LocalDateTime.now());
        assertThat(calculator.calculate(q4, LocalDateTime.now()).score()).isEqualTo(30);
    }

    @Test
    void keywordScore_urgentKeywordsScore25() {
        Product product = new Product(seller, "Item", "Misc", new BigDecimal("10.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now());
        order.addItem(product, 1);

        String[] urgentKeywords = {"urgent", "refund", "cancel", "not received", "broken"};
        for (String keyword : urgentKeywords) {
            Question q = new Question(order, buyer, product, "Test " + keyword + " message", LocalDateTime.now());
            PriorityResult result = calculator.calculate(q, LocalDateTime.now());
            assertThat(result.reasons()).as("Keyword '%s' should trigger urgent score", keyword)
                    .contains("Message contains urgent support keywords");
        }
    }

    @Test
    void keywordScore_administrativeKeywordsScore10() {
        Product product = new Product(seller, "Item", "Misc", new BigDecimal("10.00"));
        Order order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now());
        order.addItem(product, 1);

        String[] adminKeywords = {"invoice", "warranty"};
        for (String keyword : adminKeywords) {
            Question q = new Question(order, buyer, product, "Test " + keyword + " message", LocalDateTime.now());
            PriorityResult result = calculator.calculate(q, LocalDateTime.now());
            assertThat(result.reasons()).as("Keyword '%s' should trigger admin score", keyword)
                    .contains("Message contains administrative support keywords");
        }
    }

    @Test
    void productCategoryScore_electronicsAndHome() {
        Seller s = seller;

        Product electronics = new Product(s, "Phone", "Electronics", new BigDecimal("10.00"));
        Order eOrder = new Order(s, buyer, OrderStatus.PAID, LocalDateTime.now());
        eOrder.addItem(electronics, 1);
        Question q1 = new Question(eOrder, buyer, electronics, "Hi", LocalDateTime.now());
        assertThat(calculator.calculate(q1, LocalDateTime.now()).reasons())
                .contains("Electronics product category");

        Product home = new Product(s, "Lamp", "Home", new BigDecimal("10.00"));
        Order hOrder = new Order(s, buyer, OrderStatus.PAID, LocalDateTime.now());
        hOrder.addItem(home, 1);
        Question q2 = new Question(hOrder, buyer, home, "Hi", LocalDateTime.now());
        assertThat(calculator.calculate(q2, LocalDateTime.now()).reasons())
                .contains("Home product category");

        Product other = new Product(s, "Book", "Books", new BigDecimal("10.00"));
        Order oOrder = new Order(s, buyer, OrderStatus.PAID, LocalDateTime.now());
        oOrder.addItem(other, 1);
        Question q3 = new Question(oOrder, buyer, other, "Hi", LocalDateTime.now());
        assertThat(calculator.calculate(q3, LocalDateTime.now()).reasons())
                .noneMatch(r -> r.contains("product category"));
    }
}
