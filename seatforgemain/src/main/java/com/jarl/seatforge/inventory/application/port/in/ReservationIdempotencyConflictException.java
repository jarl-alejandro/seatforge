package com.jarl.seatforge.inventory.application.port.in;

public final class ReservationIdempotencyConflictException extends RuntimeException {
    public ReservationIdempotencyConflictException() {
        super("idempotency key is already bound to a different request");
    }
}
