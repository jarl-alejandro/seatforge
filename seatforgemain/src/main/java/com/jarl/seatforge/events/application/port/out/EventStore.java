package com.jarl.seatforge.events.application.port.out;

import com.jarl.seatforge.events.domain.Event;

public interface EventStore {
    void save(Event event);
}
