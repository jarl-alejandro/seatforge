package com.jarl.seatforge.inventory;

import com.jarl.seatforge.inventory.application.port.out.ExpiredReservationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "seatforge.inventory.expiration.enabled=false"
})
class ExpiredReservationPostgresIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-07-26T15:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired ExpiredReservationStore store;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private final AtomicInteger ticketNumber = new AtomicInteger();

    @BeforeEach
    void clean() {
        jdbc.update("delete from reservations");
        jdbc.update("delete from tickets");
        jdbc.update("delete from events");
        ticketNumber.set(0);
    }

    @Test
    void releases_only_expired_reservations_in_configured_batches_and_retry_is_idempotent() {
        UUID eventId = event();
        UUID expired1 = reservation(eventId, "RESERVED", NOW.minusSeconds(1));
        UUID expiredAtCutoff = reservation(eventId, "RESERVED", NOW);
        UUID future = reservation(eventId, "RESERVED", NOW.plusSeconds(1));
        UUID sold = reservation(eventId, "SOLD", NOW.minusSeconds(60));

        var first = store.releaseExpired(NOW, 1);
        assertThat(first).isEqualTo(new ExpiredReservationStore.BatchOutcome(1, 1, 0));
        assertThat(countTicketStatus("AVAILABLE")).isEqualTo(1);

        var second = store.releaseExpired(NOW, 10);
        assertThat(second).isEqualTo(new ExpiredReservationStore.BatchOutcome(1, 1, 0));
        assertThat(ticketStatus(expired1)).isEqualTo("AVAILABLE");
        assertThat(ticketStatus(expiredAtCutoff)).isEqualTo("AVAILABLE");
        assertThat(ticketStatus(future)).isEqualTo("RESERVED");
        assertThat(ticketStatus(sold)).isEqualTo("SOLD");
        assertThat(reservationStatus(future)).isEqualTo("ACTIVE");
        assertThat(reservationStatus(sold)).isEqualTo("ACTIVE");

        assertThat(store.releaseExpired(NOW, 10))
                .isEqualTo(new ExpiredReservationStore.BatchOutcome(0, 0, 0));
        assertThat(countTicketStatus("AVAILABLE")).isEqualTo(2);
    }

    @Test
    void expiration_racing_confirmation_finishes_available_or_sold_without_confirmed_available() throws Exception {
        UUID eventId = event();
        for (int attempt = 0; attempt < 20; attempt++) {
            UUID ticketId = reservation(eventId, "RESERVED", NOW.minusSeconds(1));
            CountDownLatch start = new CountDownLatch(1);
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                Future<ExpiredReservationStore.BatchOutcome> expiration = executor.submit(() -> {
                    start.await();
                    return store.releaseExpired(NOW, 1);
                });
                Future<Boolean> confirmation = executor.submit(() -> {
                    start.await();
                    return confirm(ticketId);
                });
                start.countDown();
                expiration.get();
                boolean confirmed = confirmation.get();
                String finalStatus = ticketStatus(ticketId);

                assertThat(finalStatus).isIn("AVAILABLE", "SOLD");
                if (confirmed) assertThat(finalStatus).isEqualTo("SOLD");
            }
        }
    }

    private boolean confirm(UUID ticketId) {
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status -> {
            var activeReservations = jdbc.queryForList("""
                    select r.reservation_id from reservations r
                    where r.ticket_id = ? and r.status = 'ACTIVE'
                    for update
                    """, UUID.class, ticketId);
            if (activeReservations.isEmpty()) return false;
            return jdbc.update("update tickets set status = 'SOLD' where ticket_id = ? and status = 'RESERVED'",
                    ticketId) == 1;
        }));
    }

    private UUID event() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into events(event_id, owner_id, name, starts_at, price_amount, currency, capacity, status)
                values (?, 'owner', 'Concert', ?, 25.00, 'USD', 100, 'PUBLISHED')
                """, id, Timestamp.from(NOW.plusSeconds(3600)));
        return id;
    }

    private UUID reservation(UUID eventId, String ticketStatus, Instant expiresAt) {
        UUID ticketId = UUID.randomUUID();
        jdbc.update("""
                insert into tickets(ticket_id, event_id, ticket_number, status, price_amount, currency)
                values (?, ?, ?, ?, ?, 'USD')
                """, ticketId, eventId, ticketNumber.incrementAndGet(), ticketStatus, new BigDecimal("25.00"));
        jdbc.update("""
                insert into reservations(reservation_id, ticket_id, buyer_id, status, expires_at,
                                         idempotency_key, request_hash)
                values (?, ?, 'buyer', 'ACTIVE', ?, ?, ?)
                """, UUID.randomUUID(), ticketId, Timestamp.from(expiresAt), UUID.randomUUID(), "a".repeat(64));
        return ticketId;
    }

    private int countTicketStatus(String status) {
        return jdbc.queryForObject("select count(*) from tickets where status = ?", Integer.class, status);
    }

    private String ticketStatus(UUID id) {
        return jdbc.queryForObject("select status from tickets where ticket_id = ?", String.class, id);
    }

    private String reservationStatus(UUID ticketId) {
        return jdbc.queryForObject("select status from reservations where ticket_id = ?", String.class, ticketId);
    }
}
