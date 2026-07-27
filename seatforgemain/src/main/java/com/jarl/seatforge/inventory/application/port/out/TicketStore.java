package com.jarl.seatforge.inventory.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TicketStore {
    int createAvailableTickets(UUID eventId, BigDecimal price, String currency, int capacity);

    boolean hasAvailableTickets(UUID eventId);

    TicketPage findByEventId(UUID eventId, int page, int size);

    void markEventPublished(UUID eventId);

    record StoredTicket(UUID ticketId, UUID eventId, int number, String status,
                        BigDecimal price, String currency) {
    }

    record TicketPage(List<StoredTicket> items, long totalElements, int totalPages) {
    }
}
