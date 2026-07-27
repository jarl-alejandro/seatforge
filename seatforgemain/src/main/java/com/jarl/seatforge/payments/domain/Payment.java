package com.jarl.seatforge.payments.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Payment(UUID id, UUID orderId, PaymentScenario result, String orderStatus,
                      String ticketStatus, Instant processedAt) {
    public Payment {
        Objects.requireNonNull(id); Objects.requireNonNull(orderId); Objects.requireNonNull(result);
        Objects.requireNonNull(orderStatus); Objects.requireNonNull(ticketStatus); Objects.requireNonNull(processedAt);
        if (result == PaymentScenario.TIMEOUT) throw new IllegalArgumentException("timeout is not a payment result");
        if (result == PaymentScenario.APPROVED && !(orderStatus.equals("CONFIRMED") && ticketStatus.equals("SOLD")))
            throw new IllegalArgumentException("approved payment must confirm and sell");
        if (result == PaymentScenario.DECLINED && !(orderStatus.equals("DECLINED") && ticketStatus.equals("AVAILABLE")))
            throw new IllegalArgumentException("declined payment must decline and release");
    }

    public static Payment terminal(UUID id, UUID orderId, PaymentScenario scenario, Instant at) {
        return scenario == PaymentScenario.APPROVED
                ? new Payment(id, orderId, scenario, "CONFIRMED", "SOLD", at)
                : new Payment(id, orderId, scenario, "DECLINED", "AVAILABLE", at);
    }
}
