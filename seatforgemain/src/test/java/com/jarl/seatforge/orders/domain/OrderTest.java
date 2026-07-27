package com.jarl.seatforge.orders.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    @Test
    void pending_order_captures_usd_total_and_buyer() {
        var order = Order.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "buyer-1", new BigDecimal("25"), "USD", Instant.EPOCH);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.totalAmount()).isEqualByComparingTo("25.00");
        assertThat(order.buyerId()).isEqualTo("buyer-1");
    }

    @Test
    void rejects_non_usd_or_invalid_totals() {
        assertThatThrownBy(() -> Order.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "buyer", BigDecimal.ZERO, "USD", Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Order.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "buyer", BigDecimal.ONE, "EUR", Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
