package com.jarl.seatforge.inventory.application.port.in;

public final class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException() {
        super("ticket does not exist or its event is not published");
    }
}
