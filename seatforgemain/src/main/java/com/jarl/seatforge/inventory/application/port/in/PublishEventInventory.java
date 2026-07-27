package com.jarl.seatforge.inventory.application.port.in;

import java.util.UUID;

public interface PublishEventInventory {
    void markPublished(UUID eventId);
}
