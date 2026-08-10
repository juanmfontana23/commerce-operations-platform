package com.example.commerceoperations.shared.exception;

public class SellerAccessDeniedException extends RuntimeException {

    public SellerAccessDeniedException() {
        super("Authenticated seller is not authorized to access this resource");
    }
}
