package com.jarl.seatforge.events.application.port.out;

import java.util.UUID;

public interface EventIdGenerator {
    UUID nextId();
}
