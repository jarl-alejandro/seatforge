package com.jarl.seatforge.inventory.application.port.in;

public final class ReservationForOrderNotFoundException extends RuntimeException {
    public ReservationForOrderNotFoundException() {
        super("The reservation does not exist");
    }
}
