package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.inventory.application.port.in.PublishEventInventory;
import com.jarl.seatforge.inventory.application.port.out.TicketStore;

import java.util.UUID;

public final class PublishEventInventoryHandler implements PublishEventInventory {
    private final TicketStore ticketStore;

    public PublishEventInventoryHandler(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public void markPublished(UUID eventId) {
        ticketStore.markEventPublished(eventId);
    }
}
