package com.example.commerceoperations.shared.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "correlationId";
    private static final int MAX_LENGTH = 64;
    private static final String VALID_ID = "[A-Za-z0-9][A-Za-z0-9._~-]{0,63}";
    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private final Supplier<String> idGenerator;

    public CorrelationIdFilter() {
        this(() -> UUID.randomUUID().toString());
    }

    CorrelationIdFilter(Supplier<String> idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = validIncomingId(request.getHeader(HEADER_NAME))
                .orElseGet(idGenerator);
        long startedAt = System.nanoTime();
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.info("http_request method={} path={} status={} durationMs={} correlationId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs, correlationId);
            MDC.remove(MDC_KEY);
        }
    }

    private java.util.Optional<String> validIncomingId(String value) {
        if (value == null || value.length() > MAX_LENGTH || !value.matches(VALID_ID)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(value);
    }
}
