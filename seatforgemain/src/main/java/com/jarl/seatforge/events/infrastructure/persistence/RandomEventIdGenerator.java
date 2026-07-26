package com.jarl.seatforge.events.infrastructure.persistence;

import com.jarl.seatforge.events.application.port.out.EventIdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RandomEventIdGenerator implements EventIdGenerator {
    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
