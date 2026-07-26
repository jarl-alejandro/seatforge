package com.jarl.seatforge.inventory.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreateEventInventory {

    int create(InventoryCommand command);

    record InventoryCommand(UUID eventId, BigDecimal price, String currency, int capacity) {
    }
}
