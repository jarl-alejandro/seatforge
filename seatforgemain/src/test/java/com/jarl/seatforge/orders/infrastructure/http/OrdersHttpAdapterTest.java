package com.jarl.seatforge.orders.infrastructure.http;

import com.jarl.seatforge.orders.application.port.in.CreateOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.GetOrderUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrdersHttpAdapter.class, properties =
        "spring.autoconfigure.exclude=org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration")
class OrdersHttpAdapterTest {
    @Autowired MockMvc mvc;
    @MockitoBean CreateOrderUseCase createOrders;
    @MockitoBean GetOrderUseCase getOrders;

    @Test
    void creates_an_order_through_the_generated_contract() throws Exception {
        UUID key = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        var order = result(reservationId);
        when(createOrders.create(reservationId, key)).thenReturn(order);

        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                        .contentType("application/json")
                        .content("{\"reservationId\":\"" + reservationId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/" + order.orderId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total.currency").value("USD"));
    }

    @Test
    void gets_an_owned_order_and_requires_the_idempotency_header_on_creation() throws Exception {
        var order = result(UUID.randomUUID());
        when(getOrders.get(order.orderId())).thenReturn(order);
        mvc.perform(get("/api/v1/orders/{orderId}", order.orderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.orderId().toString()));

        mvc.perform(post("/api/v1/orders").contentType("application/json")
                        .content("{\"reservationId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    private static CreateOrderUseCase.OrderResult result(UUID reservationId) {
        return new CreateOrderUseCase.OrderResult(UUID.randomUUID(), reservationId, UUID.randomUUID(),
                new BigDecimal("25.00"), "USD", "PENDING",
                Instant.parse("2030-01-01T00:00:00Z"));
    }
}
