package com.jarl.seatforge.events.application.usecase;

import com.jarl.seatforge.events.application.port.in.BrowseCatalogUseCase;
import com.jarl.seatforge.events.application.port.in.EventNotFoundException;
import com.jarl.seatforge.events.application.port.out.EventStore;
import com.jarl.seatforge.events.domain.Event;
import com.jarl.seatforge.events.domain.EventStatus;
import com.jarl.seatforge.inventory.application.port.in.QueryEventInventory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class BrowseCatalogHandler implements BrowseCatalogUseCase {
    private final EventStore eventStore;
    private final QueryEventInventory inventory;
    private final Clock clock;

    public BrowseCatalogHandler(EventStore eventStore, QueryEventInventory inventory, Clock clock) {
        this.eventStore = eventStore;
        this.inventory = inventory;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public EventPage listEvents(int page, int size) {
        validatePage(page, size);
        var result = eventStore.findPublishedAfter(clock.instant(), page, size);
        return new EventPage(page, size, result.totalElements(), result.totalPages(),
                result.items().stream().map(BrowseCatalogHandler::toCatalogEvent).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogEvent getEvent(UUID eventId) {
        return toCatalogEvent(findPublicEvent(eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketPage listTickets(UUID eventId, int page, int size) {
        validatePage(page, size);
        findPublicEvent(eventId);
        var result = inventory.listTickets(eventId, page, size);
        return new TicketPage(page, size, result.totalElements(), result.totalPages(),
                result.items().stream().map(ticket -> new CatalogTicket(
                        ticket.ticketId(), ticket.eventId(), ticket.number(), ticket.status(),
                        ticket.price(), ticket.currency())).toList());
    }

    private Event findPublicEvent(UUID eventId) {
        Event event = eventStore.findById(eventId).orElseThrow(EventNotFoundException::new);
        if (event.status() != EventStatus.PUBLISHED || !event.startsAt().isAfter(clock.instant())) {
            throw new EventNotFoundException();
        }
        return event;
    }

    private static CatalogEvent toCatalogEvent(Event event) {
        return new CatalogEvent(event.id(), event.name(), event.startsAt(), event.price().amount(),
                event.price().currency(), event.capacity(), event.status().name());
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
    }
}
