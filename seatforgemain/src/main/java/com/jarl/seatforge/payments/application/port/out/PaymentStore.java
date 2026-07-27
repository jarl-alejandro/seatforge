package com.jarl.seatforge.payments.application.port.out;

import com.jarl.seatforge.payments.domain.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentStore {
    void lockIdempotencyKey(UUID key);
    Optional<StoredPayment> findByIdempotencyKey(UUID key);
    Optional<StoredPayment> findByOrderId(UUID orderId);
    Optional<OrderForPayment> findOwnedOrderForUpdate(UUID orderId, String buyerId);
    void apply(Payment payment, UUID idempotencyKey, String requestHash);
    record StoredPayment(Payment payment, String requestHash) {}
    record OrderForPayment(UUID orderId, UUID ticketId, String status, String ticketStatus) {}
}
