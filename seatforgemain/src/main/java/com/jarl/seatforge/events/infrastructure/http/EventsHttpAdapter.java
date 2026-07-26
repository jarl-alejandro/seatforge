package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.contract.api.EventsApi;
import com.jarl.seatforge.contract.model.CreateEventRequest;
import com.jarl.seatforge.contract.model.Event;
import com.jarl.seatforge.contract.model.EventPage;
import com.jarl.seatforge.contract.model.Money;
import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EventsHttpAdapter implements EventsApi {

    private final CreateEventUseCase createEventUseCase;

    public EventsHttpAdapter(CreateEventUseCase createEventUseCase) {
        this.createEventUseCase = createEventUseCase;
    }

    @Override
    public ResponseEntity<Event> createEvent(CreateEventRequest request) {
        CreateEventUseCase.CreatedEvent created = createEventUseCase.create(
                new CreateEventUseCase.CreateEventCommand(
                        request.getName(),
                        request.getStartsAt().toInstant(),
                        request.getPrice().getAmount(),
                        request.getPrice().getCurrency().getValue(),
                        request.getCapacity()
                ));

        Event response = new Event(
                created.eventId(),
                created.name(),
                OffsetDateTime.ofInstant(created.startsAt(), ZoneOffset.UTC),
                new Money(created.price(), Money.CurrencyEnum.fromValue(created.currency())),
                created.createdTicketCount(),
                Event.StatusEnum.fromValue(created.status())
        );

        return ResponseEntity.created(URI.create("/api/v1/events/" + created.eventId())).body(response);
    }

    @Override
    public ResponseEntity<Event> getPublishedEvent(UUID eventId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<EventPage> listPublishedEvents(Integer page, Integer size) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Event> publishEvent(UUID eventId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
