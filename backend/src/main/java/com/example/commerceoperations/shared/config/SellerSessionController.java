package com.example.commerceoperations.shared.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.commerceoperations.shared.security.SellerAuthorization;

@RestController
public class SellerSessionController {

    private final SellerAuthorization sellerAuthorization;

    public SellerSessionController(SellerAuthorization sellerAuthorization) {
        this.sellerAuthorization = sellerAuthorization;
    }

    @GetMapping("/api/session")
    SellerSessionResponse currentSession(Authentication authentication) {
        return new SellerSessionResponse(sellerAuthorization.authenticatedSellerId(authentication));
    }
}
