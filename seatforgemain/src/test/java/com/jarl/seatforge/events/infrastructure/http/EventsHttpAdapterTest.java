package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
import com.jarl.seatforge.events.application.port.in.BrowseCatalogUseCase;
import com.jarl.seatforge.events.application.port.in.PublishEventUseCase;
import com.jarl.seatforge.events.application.port.in.EventNotFoundException;
import com.jarl.seatforge.events.application.port.in.EventPublicationConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventsHttpAdapter.class, properties =
        "spring.autoconfigure.exclude=org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration")
class EventsHttpAdapterTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CreateEventUseCase createEventUseCase;

    @MockitoBean
    PublishEventUseCase publishEventUseCase;

    @MockitoBean
    BrowseCatalogUseCase browseCatalogUseCase;

    @Test
    void implements_the_generated_create_event_contract() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(createEventUseCase.create(any())).thenReturn(new CreateEventUseCase.CreatedEvent(
                eventId, "Concert", Instant.parse("2030-01-02T12:00:00Z"),
                new BigDecimal("25.00"), "USD", 2, "DRAFT", 2));

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Concert","startsAt":"2030-01-02T12:00:00Z",
                                 "price":{"amount":25.00,"currency":"USD"},"capacity":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/events/" + eventId))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.capacity").value(2));

        verify(createEventUseCase).create(any(CreateEventUseCase.CreateEventCommand.class));
    }

    @Test
    void generated_dto_validation_rejects_invalid_capacity() throws Exception {
        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Concert","startsAt":"2030-01-02T12:00:00Z",
                                 "price":{"amount":25.00,"currency":"USD"},"capacity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EVENT"));
    }

    @Test
    void implements_publish_and_public_catalog_contracts() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant startsAt = Instant.parse("2030-01-02T12:00:00Z");
        var published = new PublishEventUseCase.PublishedEvent(
                eventId, "Concert", startsAt, new BigDecimal("25.00"), "USD", 2, "PUBLISHED");
        var catalogEvent = new BrowseCatalogUseCase.CatalogEvent(
                eventId, "Concert", startsAt, new BigDecimal("25.00"), "USD", 2, "PUBLISHED");
        when(publishEventUseCase.publish(eventId)).thenReturn(published);
        when(browseCatalogUseCase.getEvent(eventId)).thenReturn(catalogEvent);
        when(browseCatalogUseCase.listEvents(0, 20)).thenReturn(
                new BrowseCatalogUseCase.EventPage(0, 20, 1, 1, java.util.List.of(catalogEvent)));

        mvc.perform(post("/api/v1/events/{eventId}/publish", eventId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/events/{eventId}", eventId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eventId").value(eventId.toString()));
        mvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].eventId").value(eventId.toString()));
    }

    @Test
    void maps_catalog_visibility_and_publication_conflicts_to_contract_problems() throws Exception {
        UUID missing = UUID.randomUUID();
        UUID conflict = UUID.randomUUID();
        when(browseCatalogUseCase.getEvent(missing)).thenThrow(new EventNotFoundException());
        when(publishEventUseCase.publish(conflict))
                .thenThrow(new EventPublicationConflictException("event is not publishable"));

        mvc.perform(get("/api/v1/events/{eventId}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
        mvc.perform(post("/api/v1/events/{eventId}/publish", conflict))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_PUBLICATION_CONFLICT"));
    }
}
