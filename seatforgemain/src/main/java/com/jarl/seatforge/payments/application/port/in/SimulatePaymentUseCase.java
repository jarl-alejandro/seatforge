package com.jarl.seatforge.payments.application.port.in;

import com.jarl.seatforge.payments.domain.PaymentScenario;
import java.time.Instant;
import java.util.UUID;

public interface SimulatePaymentUseCase {
    PaymentResult simulate(UUID orderId, UUID idempotencyKey, PaymentScenario scenario, int delayMs);
    record PaymentResult(UUID paymentId, UUID orderId, String result, String orderStatus,
                         String ticketStatus, Instant processedAt) {}
}
