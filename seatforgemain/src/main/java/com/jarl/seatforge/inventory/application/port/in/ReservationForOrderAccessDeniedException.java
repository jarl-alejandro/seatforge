package com.jarl.seatforge.inventory.application.port.in;

public final class ReservationForOrderAccessDeniedException extends RuntimeException {
    public ReservationForOrderAccessDeniedException() {
        super("The authenticated buyer does not own the reservation");
    }
}
