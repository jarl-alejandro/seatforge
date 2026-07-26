package com.jarl.seatforge.events;

import com.jarl.seatforge.events.application.port.in.BrowseCatalogUseCase;
import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
import com.jarl.seatforge.events.application.port.in.EventNotFoundException;
import com.jarl.seatforge.events.application.port.in.PublishEventUseCase;
import com.jarl.seatforge.events.application.port.out.EventIdGenerator;
import com.jarl.seatforge.identity.application.port.in.ActorAccessDeniedException;
import com.jarl.seatforge.identity.application.port.in.ActorId;
import com.jarl.seatforge.identity.application.port.in.ActorRole;
import com.jarl.seatforge.identity.application.port.in.AuthenticatedActor;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
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
class EventLifecyclePostgresIntegrationTest {
    private static final String OWNER = "organizer@clients";
    private static final Instant FIRST_START = Instant.parse("2030-01-02T12:00:00Z");
    private static final Instant SECOND_START = Instant.parse("2030-01-03T12:00:00Z");

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired CreateEventUseCase createEvents;
    @Autowired PublishEventUseCase publishEvents;
    @Autowired BrowseCatalogUseCase catalog;
    @Autowired MutableCurrentActor actor;
    @Autowired JdbcTemplate jdbc;

    private ExecutorService executor;

    @BeforeEach
    void prepare() {
        jdbc.update("delete from tickets");
        jdbc.update("delete from events");
        actor.actorId = OWNER;
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    void only_the_owner_can_publish_and_missing_events_are_not_found() {
        UUID eventId = create("Owned", FIRST_START, 2);
        actor.actorId = "another@clients";

        assertThatThrownBy(() -> publishEvents.publish(eventId))
                .isInstanceOf(ActorAccessDeniedException.class);
        assertThatThrownBy(() -> publishEvents.publish(UUID.randomUUID()))
                .isInstanceOf(EventNotFoundException.class);
        assertThat(statusOf(eventId)).isEqualTo("DRAFT");
    }

    @Test
    void two_concurrent_publications_converge_on_one_published_state() throws Exception {
        UUID eventId = create("Concurrent", FIRST_START, 2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        var first = executor.submit(() -> publishTogether(eventId, ready, start));
        var second = executor.submit(() -> publishTogether(eventId, ready, start));
        ready.await();
        start.countDown();

        assertThat(first.get().status()).isEqualTo("PUBLISHED");
        assertThat(second.get().status()).isEqualTo("PUBLISHED");
        assertThat(statusOf(eventId)).isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject("select count(*) from events where event_id = ?",
                Integer.class, eventId)).isEqualTo(1);
    }

    @Test
    void catalog_is_public_future_stable_paginated_and_hides_non_public_events() {
        UUID second = create("Second", SECOND_START, 3);
        UUID first = create("First", FIRST_START, 2);
        UUID draft = create("Draft", FIRST_START.plusSeconds(60), 1);
        publishEvents.publish(second);
        publishEvents.publish(first);
        insertPastPublishedEvent();

        var firstPage = catalog.listEvents(0, 1);
        var secondPage = catalog.listEvents(1, 1);
        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).extracting(BrowseCatalogUseCase.CatalogEvent::eventId)
                .containsExactly(first);
        assertThat(secondPage.items()).extracting(BrowseCatalogUseCase.CatalogEvent::eventId)
                .containsExactly(second);
        assertThat(catalog.getEvent(first).status()).isEqualTo("PUBLISHED");
        assertThatThrownBy(() -> catalog.getEvent(draft)).isInstanceOf(EventNotFoundException.class);
        assertThatThrownBy(() -> catalog.getEvent(UUID.randomUUID())).isInstanceOf(EventNotFoundException.class);

        var tickets = catalog.listTickets(second, 1, 2);
        assertThat(tickets.totalElements()).isEqualTo(3);
        assertThat(tickets.items()).extracting(BrowseCatalogUseCase.CatalogTicket::number)
                .containsExactly(3);
        assertThatThrownBy(() -> catalog.listTickets(draft, 0, 20))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void catalog_rejects_page_and_size_outside_contract_limits() {
        assertThatThrownBy(() -> catalog.listEvents(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.listEvents(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.listEvents(0, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    private PublishEventUseCase.PublishedEvent publishTogether(
            UUID eventId, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return publishEvents.publish(eventId);
    }

    private UUID create(String name, Instant startsAt, int capacity) {
        return createEvents.create(new CreateEventUseCase.CreateEventCommand(
                name, startsAt, new BigDecimal("25.00"), "USD", capacity)).eventId();
    }

    private String statusOf(UUID eventId) {
        return jdbc.queryForObject("select status from events where event_id = ?", String.class, eventId);
    }

    private void insertPastPublishedEvent() {
        jdbc.update("""
                insert into events(event_id, owner_id, name, starts_at, price_amount, currency, capacity, status)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), OWNER, "Past", Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                new BigDecimal("10.00"), "USD", 1, "PUBLISHED");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestActors {
        @Bean @Primary
        MutableCurrentActor mutableCurrentActor() {
            return new MutableCurrentActor();
        }

        @Bean @Primary
        EventIdGenerator randomTestEventIdGenerator() {
            return UUID::randomUUID;
        }
    }

    static final class MutableCurrentActor implements CurrentActor {
        volatile String actorId = OWNER;

        @Override
        public AuthenticatedActor get() {
            return new AuthenticatedActor(new ActorId(actorId), Set.of(ActorRole.ORGANIZER),
                    Set.of("create:events", "publish:events"));
        }
    }
}
