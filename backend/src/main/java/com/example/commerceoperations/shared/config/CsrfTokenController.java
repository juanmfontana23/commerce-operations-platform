package com.example.commerceoperations.shared.config;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfTokenController {

    private final CookieCsrfTokenRepository tokenRepository;

    public CsrfTokenController(CookieCsrfTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/api/csrf-token")
    Map<String, String> csrfToken(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = tokenRepository.loadToken(request);
        if (token == null) {
            token = tokenRepository.generateToken(request);
            tokenRepository.saveToken(token, request, response);
        }
        return Map.of("token", token.getToken());
    }
}
