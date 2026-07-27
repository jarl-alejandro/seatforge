package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.contract.api.InventoryApi;
import com.jarl.seatforge.contract.model.Money;
import com.jarl.seatforge.contract.model.Reservation;
import com.jarl.seatforge.contract.model.Ticket;
import com.jarl.seatforge.contract.model.TicketPage;
import com.jarl.seatforge.events.application.port.in.BrowseCatalogUseCase;
import com.jarl.seatforge.inventory.application.port.in.ReserveTicketUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EventTicketsHttpAdapter implements InventoryApi {
    private final BrowseCatalogUseCase catalog;
    private final ReserveTicketUseCase reservations;

    public EventTicketsHttpAdapter(BrowseCatalogUseCase catalog,
                                   ReserveTicketUseCase reservations) {
        this.catalog = catalog;
        this.reservations = reservations;
    }

    @Override
    public ResponseEntity<TicketPage> listEventTickets(UUID eventId, Integer page, Integer size) {
        var result = catalog.listTickets(eventId, page, size);
        var items = result.items().stream().map(ticket -> new Ticket(
                ticket.ticketId(), ticket.eventId(), ticket.number(),
                Ticket.StatusEnum.fromValue(ticket.status()),
                new Money(ticket.price(), Money.CurrencyEnum.fromValue(ticket.currency())))).toList();
        return ResponseEntity.ok(new TicketPage(result.page(), result.size(),
                result.totalElements(), result.totalPages(), items));
    }

    @Override
    public ResponseEntity<Reservation> reserveTicket(UUID ticketId, UUID idempotencyKey) {
        var reserved = reservations.reserve(ticketId, idempotencyKey);
        var body = new Reservation(reserved.reservationId(), reserved.ticketId(),
                Reservation.StatusEnum.fromValue(reserved.status()),
                OffsetDateTime.ofInstant(reserved.expiresAt(), ZoneOffset.UTC));
        return ResponseEntity.created(URI.create(
                "/api/v1/tickets/" + ticketId + "/reservations/" + reserved.reservationId()))
                .body(body);
    }
}
