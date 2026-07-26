package com.jarl.seatforge.events.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BrowseCatalogUseCase {
    EventPage listEvents(int page, int size);
    CatalogEvent getEvent(UUID eventId);
    TicketPage listTickets(UUID eventId, int page, int size);

    record CatalogEvent(UUID eventId, String name, Instant startsAt, BigDecimal price,
                        String currency, int capacity, String status) {
    }
    record CatalogTicket(UUID ticketId, UUID eventId, int number, String status,
                         BigDecimal price, String currency) {
    }
    record EventPage(int page, int size, long totalElements, int totalPages,
                     List<CatalogEvent> items) {
    }
    record TicketPage(int page, int size, long totalElements, int totalPages,
                      List<CatalogTicket> items) {
    }
}
