package com.jarl.seatforge.payments.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PaymentTest {
    @Test void approved_is_confirmed_and_sold() {
        var payment=Payment.terminal(UUID.randomUUID(),UUID.randomUUID(),PaymentScenario.APPROVED,Instant.EPOCH);
        assertThat(payment.orderStatus()).isEqualTo("CONFIRMED"); assertThat(payment.ticketStatus()).isEqualTo("SOLD");
    }
    @Test void declined_is_declined_and_available() {
        var payment=Payment.terminal(UUID.randomUUID(),UUID.randomUUID(),PaymentScenario.DECLINED,Instant.EPOCH);
        assertThat(payment.orderStatus()).isEqualTo("DECLINED"); assertThat(payment.ticketStatus()).isEqualTo("AVAILABLE");
    }
    @Test void timeout_cannot_be_persisted() {
        assertThatThrownBy(()->Payment.terminal(UUID.randomUUID(),UUID.randomUUID(),PaymentScenario.TIMEOUT,Instant.EPOCH)).isInstanceOf(IllegalArgumentException.class);
    }
}
