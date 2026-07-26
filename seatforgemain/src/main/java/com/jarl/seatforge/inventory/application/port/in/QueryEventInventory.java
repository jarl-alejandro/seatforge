package com.jarl.seatforge.inventory.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface QueryEventInventory {
    boolean hasAvailableTickets(UUID eventId);
    TicketPage listTickets(UUID eventId, int page, int size);

    record TicketView(UUID ticketId, UUID eventId, int number, String status,
                      BigDecimal price, String currency) {
    }

    record TicketPage(int page, int size, long totalElements, int totalPages,
                      List<TicketView> items) {
    }
}
