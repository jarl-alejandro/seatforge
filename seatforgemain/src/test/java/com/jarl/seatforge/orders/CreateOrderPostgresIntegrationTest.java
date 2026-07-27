package com.jarl.seatforge.orders;

import com.jarl.seatforge.identity.application.port.in.ActorId;
import com.jarl.seatforge.identity.application.port.in.ActorRole;
import com.jarl.seatforge.identity.application.port.in.AuthenticatedActor;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderAccessDeniedException;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderConflictException;
import com.jarl.seatforge.orders.application.port.in.CreateOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.GetOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.OrderConflictException;
import com.jarl.seatforge.orders.application.port.in.OrderIdempotencyConflictException;
import com.jarl.seatforge.orders.application.port.in.OrderNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"
})
class CreateOrderPostgresIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired CreateOrderUseCase createOrders;
    @Autowired GetOrderUseCase getOrders;
    @Autowired JdbcTemplate jdbc;
    @Autowired ThreadLocalCurrentActor actors;
    private ExecutorService executor;

    @BeforeEach
    void prepare() {
        jdbc.update("delete from purchase_orders");
        jdbc.update("delete from reservations");
        jdbc.update("delete from tickets");
        jdbc.update("delete from events");
        actors.set("buyer-main");
        executor = Executors.newFixedThreadPool(50);
    }

    @AfterEach
    void cleanup() {
        executor.shutdownNow();
        actors.clear();
    }

    @Test
    void enforces_ownership_expiration_and_owner_only_queries() {
        UUID owned = insertReservation("buyer-main", "ACTIVE", Instant.now().plusSeconds(300));
        UUID expired = insertReservation("buyer-main", "ACTIVE", Instant.now().minusSeconds(1));
        UUID other = insertReservation("other", "ACTIVE", Instant.now().plusSeconds(300));

        var order = createOrders.create(owned, UUID.randomUUID());
        assertThat(order.status()).isEqualTo("PENDING");
        assertThat(order.currency()).isEqualTo("USD");
        assertThat(getOrders.get(order.orderId())).isEqualTo(order);

        assertThatThrownBy(() -> createOrders.create(expired, UUID.randomUUID()))
                .isInstanceOf(ReservationForOrderConflictException.class);
        assertThatThrownBy(() -> createOrders.create(other, UUID.randomUUID()))
                .isInstanceOf(ReservationForOrderAccessDeniedException.class);

        actors.set("other");
        assertThatThrownBy(() -> getOrders.get(order.orderId()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void idempotency_replays_and_rejects_a_different_payload() {
        UUID first = insertReservation("buyer-main", "ACTIVE", Instant.now().plusSeconds(300));
        UUID second = insertReservation("buyer-main", "ACTIVE", Instant.now().plusSeconds(300));
        UUID key = UUID.randomUUID();

        var created = createOrders.create(first, key);
        assertThat(createOrders.create(first, key)).isEqualTo(created);
        assertThat(countOrders()).isEqualTo(1);
        assertThatThrownBy(() -> createOrders.create(second, key))
                .isInstanceOf(OrderIdempotencyConflictException.class);
        assertThatThrownBy(() -> createOrders.create(first, UUID.randomUUID()))
                .isInstanceOf(OrderConflictException.class);
    }

    @Test
    void fifty_concurrent_requests_create_at_most_one_order_for_one_reservation() throws Exception {
        UUID reservationId = insertReservation("buyer-main", "ACTIVE", Instant.now().plusSeconds(300));
        CountDownLatch ready = new CountDownLatch(50);
        CountDownLatch start = new CountDownLatch(1);
        var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
        for (int request = 0; request < 50; request++) {
            futures.add(executor.submit(() -> {
                actors.set("buyer-main");
                ready.countDown();
                start.await();
                try {
                    createOrders.create(reservationId, UUID.randomUUID());
                    return true;
                } catch (OrderConflictException expected) {
                    return false;
                } finally {
                    actors.clear();
                }
            }));
        }
        ready.await();
        start.countDown();
        int successes = 0;
        for (var future : futures) if (future.get()) successes++;
        assertThat(successes).isEqualTo(1);
        assertThat(countOrders()).isEqualTo(1);
    }

    private UUID insertReservation(String buyerId, String status, Instant expiresAt) {
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        jdbc.update("""
                insert into events(event_id, owner_id, name, starts_at, price_amount, currency,
                    capacity, status) values (?, 'organizer', 'Concert', ?, 25.00, 'USD', 1, 'PUBLISHED')
                """, eventId, Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")));
        jdbc.update("""
                insert into tickets(ticket_id, event_id, ticket_number, status, price_amount,
                    currency, event_published) values (?, ?, 1, 'RESERVED', 25.00, 'USD', true)
                """, ticketId, eventId);
        jdbc.update("""
                insert into reservations(reservation_id, ticket_id, buyer_id, status, expires_at,
                    idempotency_key, request_hash) values (?, ?, ?, ?, ?, ?, ?)
                """, reservationId, ticketId, buyerId, status, Timestamp.from(expiresAt),
                UUID.randomUUID(), "a".repeat(64));
        return reservationId;
    }

    private int countOrders() {
        return jdbc.queryForObject("select count(*) from purchase_orders", Integer.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ActorConfiguration {
        @Bean @Primary ThreadLocalCurrentActor actor() { return new ThreadLocalCurrentActor(); }
    }

    static final class ThreadLocalCurrentActor implements CurrentActor {
        private final ThreadLocal<String> buyer = new ThreadLocal<>();
        void set(String id) { buyer.set(id); }
        void clear() { buyer.remove(); }
        @Override public AuthenticatedActor get() {
            return new AuthenticatedActor(new ActorId(buyer.get()), Set.of(ActorRole.BUYER),
                    Set.of("create:orders", "read:orders"));
        }
    }
}
