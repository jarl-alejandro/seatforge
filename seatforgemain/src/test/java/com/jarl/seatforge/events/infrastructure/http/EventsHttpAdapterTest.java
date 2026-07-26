package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
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
}
