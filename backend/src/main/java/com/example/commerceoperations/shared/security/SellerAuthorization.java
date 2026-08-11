package com.example.commerceoperations.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.shared.config.SecurityUsersProperties;
import com.example.commerceoperations.shared.exception.SellerAccessDeniedException;

@Component
public class SellerAuthorization {

    private final SecurityUsersProperties properties;

    public SellerAuthorization(SecurityUsersProperties properties) {
        this.properties = properties;
    }

    public void requireSeller(Authentication authentication, Long sellerId) {
        if (sellerId != null && sellerId.equals(sellerId(authentication))) {
            return;
        }
        throw new SellerAccessDeniedException();
    }

    public void requireOrder(Authentication authentication, Order order) {
        requireSeller(authentication, order.getSeller().getId());
    }

    public void requireQuestion(Authentication authentication, Question question) {
        requireOrder(authentication, question.getOrder());
    }

    public Long authenticatedSellerId(Authentication authentication) {
        return sellerId(authentication);
    }

    private Long sellerId(Authentication authentication) {
        var user = properties.getUsers().get(authentication.getName());
        return user == null ? null : user.sellerId();
    }
}
