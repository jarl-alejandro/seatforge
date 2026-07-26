package com.jarl.seatforge.events.infrastructure.persistence;

import com.jarl.seatforge.events.application.port.out.EventStore;
import com.jarl.seatforge.events.domain.Event;
import org.springframework.stereotype.Component;

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
}
