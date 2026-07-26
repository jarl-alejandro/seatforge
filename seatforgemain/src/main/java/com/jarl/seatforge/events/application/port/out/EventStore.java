package com.jarl.seatforge.events.application.port.out;

import com.jarl.seatforge.events.domain.Event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventStore {
    void save(Event event);

    Optional<Event> findById(UUID eventId);

    EventPage findPublishedAfter(Instant instant, int page, int size);

    record EventPage(List<Event> items, long totalElements, int totalPages) {
    }
}
