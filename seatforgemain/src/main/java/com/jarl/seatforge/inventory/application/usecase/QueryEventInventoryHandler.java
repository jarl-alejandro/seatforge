package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.inventory.application.port.in.QueryEventInventory;
import com.jarl.seatforge.inventory.application.port.out.TicketStore;

import java.util.UUID;

public final class QueryEventInventoryHandler implements QueryEventInventory {
    private final TicketStore ticketStore;

    public QueryEventInventoryHandler(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public boolean hasAvailableTickets(UUID eventId) {
        return ticketStore.hasAvailableTickets(eventId);
    }

    @Override
    public TicketPage listTickets(UUID eventId, int page, int size) {
        var result = ticketStore.findByEventId(eventId, page, size);
        return new TicketPage(page, size, result.totalElements(), result.totalPages(),
                result.items().stream().map(ticket -> new TicketView(
                        ticket.ticketId(), ticket.eventId(), ticket.number(), ticket.status(),
                        ticket.price(), ticket.currency())).toList());
    }
}
