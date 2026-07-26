package com.jarl.seatforge.inventory.infrastructure.persistence;

import com.jarl.seatforge.inventory.application.port.out.TicketStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class JpaTicketStoreAdapter implements TicketStore {

    private static final int BATCH_SIZE = 500;
    private final EntityManager entityManager;

    public JpaTicketStoreAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public int createAvailableTickets(UUID eventId, BigDecimal price, String currency, int capacity) {
        for (int number = 1; number <= capacity; number++) {
            entityManager.persist(new TicketJpaEntity(
                    UUID.randomUUID(), eventId, number, price, currency));
            if (number % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        return capacity;
    }
}
