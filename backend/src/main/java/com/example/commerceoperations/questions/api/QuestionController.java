package com.example.commerceoperations.questions.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.example.commerceoperations.questions.application.QuestionService;
import com.example.commerceoperations.orders.application.OrderService;
import com.example.commerceoperations.shared.security.SellerAuthorization;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;
    private final OrderService orderService;
    private final SellerAuthorization sellerAuthorization;

    public QuestionController(QuestionService questionService, OrderService orderService, SellerAuthorization sellerAuthorization) {
        this.questionService = questionService;
        this.orderService = orderService;
        this.sellerAuthorization = sellerAuthorization;
    }

    @GetMapping("/orders/{orderId}/questions")
    List<QuestionResponse> findOrderQuestions(@PathVariable Long orderId, Authentication authentication) {
        var order = orderService.getOrder(orderId);
        sellerAuthorization.requireOrder(authentication, order);
        var questions = questionService.findOrderQuestions(orderId);
        return questions.stream()
                .map(item -> QuestionMapper.toResponse(item.question(), item.priority()))
                .toList();
    }

    @GetMapping("/sellers/{sellerId}/questions/unresolved")
    List<QuestionResponse> findUnresolvedSellerQuestions(@PathVariable Long sellerId, Authentication authentication) {
        sellerAuthorization.requireSeller(authentication, sellerId);
        return questionService.findUnresolvedSellerQuestions(sellerId).stream()
                .map(item -> QuestionMapper.toResponse(item.question(), item.priority()))
                .toList();
    }

    @PostMapping("/questions/{questionId}/answer")
    QuestionResponse answerQuestion(@PathVariable Long questionId, @Valid @RequestBody AnswerQuestionRequest request, Authentication authentication) {
        sellerAuthorization.requireQuestion(authentication, questionService.getQuestion(questionId));
        var item = questionService.answerQuestion(questionId, request.answer());
        return QuestionMapper.toResponse(item.question(), item.priority());
    }

    @PostMapping("/questions/{questionId}/resolve")
    QuestionResponse resolveQuestion(@PathVariable Long questionId, Authentication authentication) {
        sellerAuthorization.requireQuestion(authentication, questionService.getQuestion(questionId));
        var item = questionService.resolveQuestion(questionId);
        return QuestionMapper.toResponse(item.question(), item.priority());
    }
}
