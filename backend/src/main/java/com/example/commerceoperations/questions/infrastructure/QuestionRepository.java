package com.example.commerceoperations.questions.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.questions.domain.QuestionStatus;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = { "order", "order.seller", "order.buyer", "order.items", "buyer", "product" })
    List<Question> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = { "order", "order.seller", "order.buyer", "order.items", "buyer", "product" })
    List<Question> findByOrderSellerIdAndStatusNot(Long sellerId, QuestionStatus status);

    @EntityGraph(attributePaths = { "order", "order.seller", "order.buyer", "order.items", "buyer", "product" })
    @Query("select q from Question q where q.id = :id")
    java.util.Optional<Question> findWithDetailsById(@Param("id") Long id);
}
