package com.jarl.seatforge.inventory.infrastructure.persistence;

import com.jarl.seatforge.inventory.application.port.out.ReservationStore;
import com.jarl.seatforge.inventory.domain.Reservation;
import com.jarl.seatforge.inventory.domain.Ticket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaReservationStoreAdapter implements ReservationStore {
    private final EntityManager entityManager;

    public JpaReservationStoreAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockIdempotencyKey(UUID idempotencyKey) {
        long lockKey = idempotencyKey.getMostSignificantBits()
                ^ idempotencyKey.getLeastSignificantBits();
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)", Object.class)
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }

    @Override
    public Optional<StoredReservation> findByIdempotencyKey(UUID idempotencyKey) {
        return entityManager.createQuery("""
                        select r from ReservationJpaEntity r
                        where r.idempotencyKey = :idempotencyKey
                        """, ReservationJpaEntity.class)
                .setParameter("idempotencyKey", idempotencyKey)
                .getResultStream()
                .findFirst()
                .map(r -> new StoredReservation(r.id, r.ticketId, r.buyerId, r.status,
                        r.expiresAt, r.requestHash));
    }

    @Override
    public Optional<Ticket> findTicketForUpdate(UUID ticketId) {
        return Optional.ofNullable(entityManager.find(
                        TicketJpaEntity.class, ticketId, LockModeType.PESSIMISTIC_WRITE))
                .map(TicketJpaEntity::toDomain);
    }

    @Override
    public void save(Ticket ticket, Reservation reservation,
                     UUID idempotencyKey, String requestHash) {
        TicketJpaEntity entity = entityManager.find(TicketJpaEntity.class, ticket.id());
        entity.apply(ticket);
        entityManager.persist(new ReservationJpaEntity(reservation, idempotencyKey, requestHash));
        entityManager.flush();
    }
}
