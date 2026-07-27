package com.jarl.seatforge.inventory;

import com.jarl.seatforge.identity.application.port.in.ActorId;
import com.jarl.seatforge.identity.application.port.in.ActorRole;
import com.jarl.seatforge.identity.application.port.in.AuthenticatedActor;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.ReservationIdempotencyConflictException;
import com.jarl.seatforge.inventory.application.port.in.ReserveTicketUseCase;
import com.jarl.seatforge.inventory.application.port.in.TicketReservationConflictException;
import com.jarl.seatforge.inventory.application.port.in.TicketNotFoundException;
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
        "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate",
        "seatforge.inventory.reservation-ttl=PT10M"
})
class ReserveTicketPostgresIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired ReserveTicketUseCase reservations;
    @Autowired JdbcTemplate jdbc;
    @Autowired ThreadLocalCurrentActor actors;

    private ExecutorService executor;

    @BeforeEach
    void prepare() {
        jdbc.update("delete from reservations");
        jdbc.update("delete from tickets");
        jdbc.update("delete from events");
        actors.set("buyer-main");
        executor = Executors.newFixedThreadPool(100);
    }

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
        actors.clear();
    }

    @Test
    void same_key_and_request_returns_the_original_reservation_but_other_payload_conflicts() {
        UUID eventId = insertPublishedEvent(2);
        UUID firstTicket = insertTicket(eventId, 1, "AVAILABLE");
        UUID secondTicket = insertTicket(eventId, 2, "AVAILABLE");
        UUID key = UUID.randomUUID();

        var first = reservations.reserve(firstTicket, key);
        var replay = reservations.reserve(firstTicket, key);

        assertThat(replay).isEqualTo(first);
        assertThat(countReservations()).isEqualTo(1);

        // Idempotency reproduces the original HTTP representation even after US-009 expires it.
        jdbc.update("update reservations set status = 'EXPIRED' where reservation_id = ?",
                first.reservationId());
        assertThat(reservations.reserve(firstTicket, key)).isEqualTo(first);

        assertThatThrownBy(() -> reservations.reserve(secondTicket, key))
                .isInstanceOf(ReservationIdempotencyConflictException.class);
        assertThat(ticketStatus(secondTicket)).isEqualTo("AVAILABLE");
    }

    @Test
    void reserved_or_sold_ticket_conflicts_without_changing_existing_ownership() {
        UUID eventId = insertPublishedEvent(2);
        UUID reserved = insertTicket(eventId, 1, "AVAILABLE");
        UUID sold = insertTicket(eventId, 2, "SOLD");
        var winner = reservations.reserve(reserved, UUID.randomUUID());

        actors.set("other-buyer");
        assertThatThrownBy(() -> reservations.reserve(reserved, UUID.randomUUID()))
                .isInstanceOf(TicketReservationConflictException.class);
        assertThatThrownBy(() -> reservations.reserve(sold, UUID.randomUUID()))
                .isInstanceOf(TicketReservationConflictException.class);

        assertThat(jdbc.queryForObject(
                "select buyer_id from reservations where reservation_id = ?",
                String.class, winner.reservationId())).isEqualTo("buyer-main");
        assertThat(ticketStatus(reserved)).isEqualTo("RESERVED");
        assertThat(ticketStatus(sold)).isEqualTo("SOLD");
    }

    @Test
    void ticket_of_an_unpublished_event_is_not_visible_for_reservation() {
        UUID eventId = insertPublishedEvent(1);
        UUID ticketId = insertTicket(eventId, 1, "AVAILABLE");
        jdbc.update("update tickets set event_published = false where ticket_id = ?", ticketId);

        assertThatThrownBy(() -> reservations.reserve(ticketId, UUID.randomUUID()))
                .isInstanceOf(TicketNotFoundException.class);
        assertThat(ticketStatus(ticketId)).isEqualTo("AVAILABLE");
        assertThat(countReservations()).isZero();
    }

    @Test
    void one_of_one_hundred_concurrent_buyers_wins_one_ticket_without_overselling() throws Exception {
        UUID eventId = insertPublishedEvent(1);
        UUID ticketId = insertTicket(eventId, 1, "AVAILABLE");
        CountDownLatch ready = new CountDownLatch(100);
        CountDownLatch start = new CountDownLatch(1);
        var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();

        for (int buyer = 0; buyer < 100; buyer++) {
            int buyerNumber = buyer;
            futures.add(executor.submit(() -> {
                actors.set("buyer-" + buyerNumber);
                ready.countDown();
                start.await();
                try {
                    reservations.reserve(ticketId, UUID.randomUUID());
                    return true;
                } catch (TicketReservationConflictException expected) {
                    return false;
                } finally {
                    actors.clear();
                }
            }));
        }
        ready.await();
        start.countDown();

        int successes = 0;
        for (var future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        assertThat(successes).isEqualTo(1);
        assertThat(countReservations()).isEqualTo(1);
        assertThat(ticketStatus(ticketId)).isEqualTo("RESERVED");
    }

    private UUID insertPublishedEvent(int capacity) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into events(event_id, owner_id, name, starts_at, price_amount,
                    currency, capacity, status) values (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "organizer", "Concert", Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")),
                new BigDecimal("25.00"), "USD", capacity, "PUBLISHED");
        return id;
    }

    private UUID insertTicket(UUID eventId, int number, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into tickets(ticket_id, event_id, ticket_number, status, price_amount, currency,
                    event_published)
                values (?, ?, ?, ?, ?, ?, true)
                """, id, eventId, number, status, new BigDecimal("25.00"), "USD");
        return id;
    }

    private int countReservations() {
        return jdbc.queryForObject("select count(*) from reservations", Integer.class);
    }

    private String ticketStatus(UUID ticketId) {
        return jdbc.queryForObject("select status from tickets where ticket_id = ?", String.class, ticketId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ActorConfiguration {
        @Bean @Primary
        ThreadLocalCurrentActor threadLocalCurrentActor() {
            return new ThreadLocalCurrentActor();
        }
    }

    static final class ThreadLocalCurrentActor implements CurrentActor {
        private final ThreadLocal<String> buyer = new ThreadLocal<>();

        void set(String buyerId) {
            buyer.set(buyerId);
        }

        void clear() {
            buyer.remove();
        }

        @Override
        public AuthenticatedActor get() {
            return new AuthenticatedActor(new ActorId(buyer.get()), Set.of(ActorRole.BUYER),
                    Set.of("reserve:tickets"));
        }
    }
}
