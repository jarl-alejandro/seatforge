package com.jarl.seatforge.payments.application.usecase;

import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.payments.application.port.in.*;
import com.jarl.seatforge.payments.application.port.out.PaymentStore;
import com.jarl.seatforge.payments.domain.Payment;
import com.jarl.seatforge.payments.domain.PaymentScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;

public class SimulatePaymentHandler implements SimulatePaymentUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(SimulatePaymentHandler.class);
    private final CurrentActor actor; private final PaymentStore store; private final Clock clock; private final int maxDelayMs;
    public SimulatePaymentHandler(CurrentActor actor, PaymentStore store, Clock clock, int maxDelayMs) {
        this.actor=actor; this.store=store; this.clock=clock; this.maxDelayMs=maxDelayMs;
    }

    @Override @Transactional
    public PaymentResult simulate(UUID orderId, UUID key, PaymentScenario scenario, int delayMs) {
        if (orderId == null || key == null || scenario == null) throw new IllegalArgumentException("payment fields are required");
        if (delayMs < 0 || delayMs > maxDelayMs) throw new IllegalArgumentException("delayMs exceeds configured limit");
        String hash = hash(orderId + ":" + scenario + ":" + delayMs);
        store.lockIdempotencyKey(key);
        var replay = store.findByIdempotencyKey(key);
        if (replay.isPresent()) {
            if (!replay.get().requestHash().equals(hash)) throw new PaymentIdempotencyConflictException();
            return result(replay.get().payment());
        }
        var order = store.findOwnedOrderForUpdate(orderId, actor.get().id().value())
                .orElseThrow(PaymentNotFoundException::new);
        var terminal = store.findByOrderId(orderId);
        if (terminal.isPresent()) return result(terminal.get().payment());
        if (!order.status().equals("PENDING") || !order.ticketStatus().equals("RESERVED"))
            throw new PaymentConflictException("Order or ticket is not payable");
        delay(delayMs);
        if (scenario == PaymentScenario.TIMEOUT) {
            LOG.warn("Simulated payment timeout orderId={} delayMs={}", orderId, delayMs);
            throw new SimulatedPaymentTimeoutException();
        }
        Payment payment = Payment.terminal(UUID.randomUUID(), orderId, scenario, clock.instant());
        store.apply(payment, key, hash);
        return result(payment);
    }
    private static void delay(int ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new SimulatedPaymentTimeoutException(); } }
    private static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static PaymentResult result(Payment p) { return new PaymentResult(p.id(),p.orderId(),p.result().name(),p.orderStatus(),p.ticketStatus(),p.processedAt()); }
}
