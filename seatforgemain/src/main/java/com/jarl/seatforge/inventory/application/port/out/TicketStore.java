package com.jarl.seatforge.inventory.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface TicketStore {
    int createAvailableTickets(UUID eventId, BigDecimal price, String currency, int capacity);
}
