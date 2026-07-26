package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.events.application.port.in.BrowseCatalogUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventTicketsHttpAdapter.class, properties =
        "spring.autoconfigure.exclude=org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration")
class EventTicketsHttpAdapterTest {
    @Autowired MockMvc mvc;
    @MockitoBean BrowseCatalogUseCase catalog;

    @Test
    void implements_the_generated_ticket_page_contract() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        when(catalog.listTickets(eventId, 0, 20)).thenReturn(
                new BrowseCatalogUseCase.TicketPage(0, 20, 1, 1, List.of(
                        new BrowseCatalogUseCase.CatalogTicket(ticketId, eventId, 1, "AVAILABLE",
                                new BigDecimal("25.00"), "USD"))));

        mvc.perform(get("/api/v1/events/{eventId}/tickets", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.items[0].number").value(1));
    }

    @Test
    void generated_contract_rejects_ticket_page_sizes_above_one_hundred() throws Exception {
        mvc.perform(get("/api/v1/events/{eventId}/tickets", UUID.randomUUID())
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest());
    }
}
