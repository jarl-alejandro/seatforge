package com.jarl.seatforge.events;

import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
import com.jarl.seatforge.events.application.port.out.EventIdGenerator;
import com.jarl.seatforge.identity.application.port.in.ActorId;
import com.jarl.seatforge.identity.application.port.in.ActorRole;
import com.jarl.seatforge.identity.application.port.in.AuthenticatedActor;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.CreateEventInventory;
import com.jarl.seatforge.inventory.application.port.out.TicketStore;
import com.jarl.seatforge.inventory.application.usecase.CreateEventInventoryHandler;
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
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class CreateEventPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    CreateEventUseCase createEventUseCase;

    @Autowired
    TestEventIdGenerator eventIdGenerator;

    @Autowired
    SwitchableInventory inventory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepare() {
        inventory.failAfterWriting = false;
        eventIdGenerator.next = UUID.randomUUID();
    }

    @Test
    void creates_exactly_numbered_unique_available_tickets() {
        CreateEventUseCase.CreatedEvent created = createEventUseCase.create(command(3));

        assertThat(created.eventId()).isEqualTo(eventIdGenerator.next);
        assertThat(created.status()).isEqualTo("DRAFT");
        assertThat(created.createdTicketCount()).isEqualTo(3);
        assertThat(count("events", eventIdGenerator.next)).isEqualTo(1);
        assertThat(count("tickets", eventIdGenerator.next)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "select count(distinct ticket_number) from tickets where event_id = ?",
                Integer.class, eventIdGenerator.next)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "select min(ticket_number) from tickets where event_id = ?",
                Integer.class, eventIdGenerator.next)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select max(ticket_number) from tickets where event_id = ?",
                Integer.class, eventIdGenerator.next)).isEqualTo(3);
    }

    @Test
    void rolls_back_event_and_inventory_when_inventory_write_fails() {
        inventory.failAfterWriting = true;

        assertThatThrownBy(() -> createEventUseCase.create(command(2)))
                .isInstanceOf(ForcedInventoryFailure.class);

        assertThat(count("events", eventIdGenerator.next)).isZero();
        assertThat(count("tickets", eventIdGenerator.next)).isZero();
    }

    private CreateEventUseCase.CreateEventCommand command(int capacity) {
        return new CreateEventUseCase.CreateEventCommand(
                "Concert", Instant.now().plusSeconds(3600),
                new BigDecimal("25.00"), "USD", capacity);
    }

    private int count(String table, UUID eventId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where event_id = ?", Integer.class, eventId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestActorsAndFailureConfiguration {

        @Bean
        @Primary
        CurrentActor testCurrentActor() {
            return () -> new AuthenticatedActor(
                    new ActorId("organizer@clients"), Set.of(ActorRole.ORGANIZER), Set.of("create:events"));
        }

        @Bean
        @Primary
        TestEventIdGenerator testEventIdGenerator() {
            return new TestEventIdGenerator();
        }

        @Bean
        @Primary
        SwitchableInventory switchableInventory(TicketStore ticketStore) {
            return new SwitchableInventory(new CreateEventInventoryHandler(ticketStore));
        }
    }

    static final class TestEventIdGenerator implements EventIdGenerator {
        UUID next;

        @Override
        public UUID nextId() {
            return next;
        }
    }

    static final class SwitchableInventory implements CreateEventInventory {
        private final CreateEventInventory delegate;
        boolean failAfterWriting;

        SwitchableInventory(CreateEventInventory delegate) {
            this.delegate = delegate;
        }

        @Override
        public int create(InventoryCommand command) {
            int count = delegate.create(command);
            if (failAfterWriting) {
                throw new ForcedInventoryFailure();
            }
            return count;
        }
    }

    static final class ForcedInventoryFailure extends RuntimeException {
    }
}
