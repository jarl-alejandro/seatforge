package com.jarl.seatforge.events.infrastructure.persistence;

import com.jarl.seatforge.events.application.port.out.EventStore;
import com.jarl.seatforge.events.domain.Event;
import org.springframework.stereotype.Component;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaEventStoreAdapter implements EventStore {

    private final SpringDataEventStore store;

    public JpaEventStoreAdapter(SpringDataEventStore store) {
        this.store = store;
    }

    @Override
    public void save(Event event) {
        store.save(new EventJpaEntity(event));
    }

    @Override
    public Optional<Event> findById(UUID eventId) {
        return store.findById(eventId).map(EventJpaEntity::toDomain);
    }

    @Override
    public EventPage findPublishedAfter(Instant instant, int page, int size) {
        var result = store.findByStatusAndStartsAtAfter(
                com.jarl.seatforge.events.domain.EventStatus.PUBLISHED,
                instant,
                PageRequest.of(page, size, Sort.by("startsAt").ascending().and(Sort.by("id").ascending())));
        return new EventPage(
                result.getContent().stream().map(EventJpaEntity::toDomain).toList(),
                result.getTotalElements(), result.getTotalPages());
    }
}
