package com.jarl.seatforge.inventory.application.port.in;

public final class ReservationForOrderConflictException extends RuntimeException {
    public ReservationForOrderConflictException() {
        super("The reservation is no longer active or has expired");
    }
}
