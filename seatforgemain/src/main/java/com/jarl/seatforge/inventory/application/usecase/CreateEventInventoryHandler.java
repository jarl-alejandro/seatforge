package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.inventory.application.port.in.CreateEventInventory;
import com.jarl.seatforge.inventory.application.port.out.TicketStore;

public final class CreateEventInventoryHandler implements CreateEventInventory {

    private final TicketStore ticketStore;

    public CreateEventInventoryHandler(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public int create(InventoryCommand command) {
        return ticketStore.createAvailableTickets(
                command.eventId(), command.price(), command.currency(), command.capacity());
    }
}
