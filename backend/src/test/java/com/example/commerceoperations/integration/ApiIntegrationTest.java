package com.example.commerceoperations.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.commerceoperations.shared.observability.CorrelationIdFilter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApiIntegrationTest {

    private static final String SELLER_ONE_AUTH = "Basic c2VsbGVyMTpzZWxsZXIxLWxvY2Fs";
    private static final String SELLER_TWO_AUTH = "Basic c2VsbGVyMjpzZWxsZXIyLWxvY2Fs";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSellerOrders_returnsOrders() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void session_returnsAuthenticatedSellerScope() throws Exception {
        mockMvc.perform(get("/api/session").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(1));
        mockMvc.perform(get("/api/session").header("Authorization", SELLER_TWO_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(2));
    }

    @Test
    void requestWithValidCorrelationId_echoesIt() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders").header("Authorization", SELLER_ONE_AUTH).header(CorrelationIdFilter.HEADER_NAME, "client-42.alpha"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "client-42.alpha"));
    }

    @Test
    void requestWithUnsafeCorrelationId_generatesReplacement() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders").header("Authorization", SELLER_ONE_AUTH).header(CorrelationIdFilter.HEADER_NAME, "bad value/with-space"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, org.hamcrest.Matchers.matchesPattern("[A-Za-z0-9-]{36}")));
    }

    @Test
    void actuatorExposesOnlyApprovedEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSellerOrders_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders").header("Authorization", SELLER_ONE_AUTH).param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Parameter 'status' must be a valid OrderStatus"))
                .andExpect(jsonPath("$.path").value("/api/sellers/1/orders"));
    }

    @Test
    void getSellerOrders_invalidDate_returns400() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders").header("Authorization", SELLER_ONE_AUTH).param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parameter 'from' must be a valid LocalDate"));
    }

    @Test
    void getOrderDetail_returnsOrderWithItems() throws Exception {
        mockMvc.perform(get("/api/orders/1").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sellerId").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.buyer.name").value("Sofia Alvarez"));
    }

    @Test
    void authenticatedSellerCanReadQuestions() throws Exception {
        mockMvc.perform(get("/api/orders/1/questions").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/sellers/1/questions/unresolved").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void answerQuestion_succeeds() throws Exception {
        mockMvc.perform(post("/api/questions/1/answer").header("Authorization", SELLER_ONE_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"test response\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.answer").value("test response"))
                .andExpect(jsonPath("$.answeredAt").isNotEmpty());
    }

    @Test
    void answerQuestion_blankAnswer_returns400() throws Exception {
        mockMvc.perform(post("/api/questions/1/answer").header("Authorization", SELLER_ONE_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("answer must not be blank"));
    }

    @Test
    void answerQuestion_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/questions/1/answer").header("Authorization", SELLER_ONE_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/99999").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getOrder_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/api/orders/not-a-number").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parameter 'orderId' must be a valid Long"));
    }

    @Test
    void answerQuestion_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/questions/1/answer").header("Authorization", SELLER_ONE_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                .andExpect(jsonPath("$.path").value("/api/questions/1/answer"));
    }

    @Test
    void transitionOrder_validTransition_succeeds() throws Exception {
        mockMvc.perform(post("/api/orders/5/transition").header("Authorization", SELLER_ONE_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void transitionOrder_invalidTransition_returns400() throws Exception {
        mockMvc.perform(post("/api/orders/3/transition").header("Authorization", SELLER_ONE_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CREATED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid status transition")));
    }

    @Test
    void transitionOrder_missingStatus_returns400() throws Exception {
        mockMvc.perform(post("/api/orders/1/transition").header("Authorization", SELLER_ONE_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiWithoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiWithInvalidAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders").header("Authorization", "Basic aW52YWxpZDp3cm9uZw=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestWithoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/api/questions/1/resolve").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isForbidden());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void csrfTokenEndpoint_returnsTokenAndCookie() throws Exception {
        var response = mockMvc.perform(get("/api/csrf-token").header("Authorization", SELLER_ONE_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("XSRF-TOKEN=")))
                .andReturn();
        String token = response.getResponse().getContentAsString().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        var cookie = response.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/questions/1/resolve").header("Authorization", SELLER_ONE_AUTH)
                        .cookie(cookie).header("X-XSRF-TOKEN", token))
                .andExpect(status().isOk());
    }

    @Test
    void crossSellerAccess_returns403() throws Exception {
        mockMvc.perform(get("/api/sellers/1/orders").header("Authorization", SELLER_TWO_AUTH))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/1/questions").header("Authorization", SELLER_TWO_AUTH))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders/1/transition").header("Authorization", SELLER_TWO_AUTH).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/questions/1/resolve").header("Authorization", SELLER_TWO_AUTH).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
