package com.jarl.seatforge.inventory.application.port.in;

public final class TicketReservationConflictException extends RuntimeException {
    public TicketReservationConflictException(String message) {
        super(message);
    }
}
