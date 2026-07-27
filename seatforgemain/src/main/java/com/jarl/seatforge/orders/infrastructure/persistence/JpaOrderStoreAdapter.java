package com.jarl.seatforge.orders.infrastructure.persistence;

import com.jarl.seatforge.orders.application.port.out.OrderStore;
import com.jarl.seatforge.orders.domain.Order;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public final class JpaOrderStoreAdapter implements OrderStore {
    private static final long ORDER_LOCK_NAMESPACE = 0x4f52444552534cL;
    private final EntityManager entityManager;

    public JpaOrderStoreAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockIdempotencyKey(UUID idempotencyKey) {
        long lockKey = idempotencyKey.getMostSignificantBits()
                ^ idempotencyKey.getLeastSignificantBits() ^ ORDER_LOCK_NAMESPACE;
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)", Object.class)
                .setParameter("lockKey", lockKey).getSingleResult();
    }

    @Override
    public Optional<StoredOrder> findByIdempotencyKey(UUID idempotencyKey) {
        return one("o.idempotencyKey = :value", idempotencyKey);
    }

    @Override
    public Optional<StoredOrder> findByReservationId(UUID reservationId) {
        return one("o.reservationId = :value", reservationId);
    }

    @Override
    public Optional<StoredOrder> findByIdAndBuyerId(UUID orderId, String buyerId) {
        return entityManager.createQuery("""
                        select o from OrderJpaEntity o
                        where o.id = :orderId and o.buyerId = :buyerId
                        """, OrderJpaEntity.class)
                .setParameter("orderId", orderId).setParameter("buyerId", buyerId)
                .getResultStream().findFirst().map(this::stored);
    }

    private Optional<StoredOrder> one(String predicate, UUID value) {
        return entityManager.createQuery("select o from OrderJpaEntity o where " + predicate,
                        OrderJpaEntity.class)
                .setParameter("value", value).getResultStream().findFirst().map(this::stored);
    }

    private StoredOrder stored(OrderJpaEntity entity) {
        return new StoredOrder(entity.toDomain(), entity.requestHash);
    }

    @Override
    public void save(Order order, UUID idempotencyKey, String requestHash) {
        entityManager.persist(new OrderJpaEntity(order, idempotencyKey, requestHash));
        entityManager.flush();
    }
}
