package com.jarl.seatforge.inventory.infrastructure.persistence;

import com.jarl.seatforge.inventory.application.port.out.TicketStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
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

    @Override
    public boolean hasAvailableTickets(UUID eventId) {
        return entityManager.createQuery("""
                        select count(t) from TicketJpaEntity t
                        where t.eventId = :eventId and t.status = 'AVAILABLE'
                        """, Long.class)
                .setParameter("eventId", eventId)
                .getSingleResult() > 0;
    }

    @Override
    public TicketPage findByEventId(UUID eventId, int page, int size) {
        long total = entityManager.createQuery(
                        "select count(t) from TicketJpaEntity t where t.eventId = :eventId", Long.class)
                .setParameter("eventId", eventId)
                .getSingleResult();
        List<StoredTicket> items = entityManager.createQuery("""
                        select t from TicketJpaEntity t
                        where t.eventId = :eventId order by t.number asc
                        """, TicketJpaEntity.class)
                .setParameter("eventId", eventId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList().stream()
                .map(t -> new StoredTicket(
                        t.id, t.eventId, t.number, t.status, t.priceAmount, t.currency))
                .toList();
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        return new TicketPage(items, total, totalPages);
    }
}
