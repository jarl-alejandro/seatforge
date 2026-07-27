package com.jarl.seatforge.orders.application.usecase;

import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.GetReservationForOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.CreateOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.OrderConflictException;
import com.jarl.seatforge.orders.application.port.in.OrderIdempotencyConflictException;
import com.jarl.seatforge.orders.application.port.out.OrderStore;
import com.jarl.seatforge.orders.domain.Order;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

public class CreateOrderHandler implements CreateOrderUseCase {
    private final CurrentActor currentActor;
    private final GetReservationForOrderUseCase reservations;
    private final OrderStore orders;
    private final Clock clock;

    public CreateOrderHandler(CurrentActor currentActor, GetReservationForOrderUseCase reservations,
                              OrderStore orders, Clock clock) {
        this.currentActor = currentActor;
        this.reservations = reservations;
        this.orders = orders;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OrderResult create(UUID reservationId, UUID idempotencyKey) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        String buyerId = currentActor.get().id().value();
        String requestHash = requestHash(reservationId);

        orders.lockIdempotencyKey(idempotencyKey);
        var replay = orders.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            var stored = replay.orElseThrow();
            if (!stored.requestHash().equals(requestHash)
                    || !stored.order().buyerId().equals(buyerId)) {
                throw new OrderIdempotencyConflictException();
            }
            return result(stored.order());
        }

        var reservation = reservations.getActiveOwnedForUpdate(reservationId, buyerId, clock.instant());
        if (orders.findByReservationId(reservationId).isPresent()) {
            throw new OrderConflictException();
        }
        var order = Order.pending(UUID.randomUUID(), reservation.reservationId(),
                reservation.ticketId(), buyerId, reservation.price(), reservation.currency(),
                clock.instant());
        orders.save(order, idempotencyKey, requestHash);
        return result(order);
    }

    static String requestHash(UUID reservationId) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(reservationId.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    static OrderResult result(Order order) {
        return new OrderResult(order.id(), order.reservationId(), order.ticketId(),
                order.totalAmount(), order.currency(), order.status().name(), order.createdAt());
    }
}
