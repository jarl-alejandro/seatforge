package com.jarl.seatforge.orders.application.port.in;

public final class OrderIdempotencyConflictException extends RuntimeException {
    public OrderIdempotencyConflictException() {
        super("The Idempotency-Key was already used with another order command");
    }
}
