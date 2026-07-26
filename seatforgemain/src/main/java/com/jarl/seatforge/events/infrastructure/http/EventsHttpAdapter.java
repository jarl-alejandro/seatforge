package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.contract.api.EventsApi;
import com.jarl.seatforge.contract.model.CreateEventRequest;
import com.jarl.seatforge.contract.model.Event;
import com.jarl.seatforge.contract.model.EventPage;
import com.jarl.seatforge.contract.model.Money;
import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
import com.jarl.seatforge.events.application.port.in.BrowseCatalogUseCase;
import com.jarl.seatforge.events.application.port.in.PublishEventUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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
    private final PublishEventUseCase publishEventUseCase;
    private final BrowseCatalogUseCase browseCatalogUseCase;

    public EventsHttpAdapter(CreateEventUseCase createEventUseCase,
                             PublishEventUseCase publishEventUseCase,
                             BrowseCatalogUseCase browseCatalogUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.publishEventUseCase = publishEventUseCase;
        this.browseCatalogUseCase = browseCatalogUseCase;
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
        return ResponseEntity.ok(toResponse(browseCatalogUseCase.getEvent(eventId)));
    }

    @Override
    public ResponseEntity<EventPage> listPublishedEvents(Integer page, Integer size) {
        var result = browseCatalogUseCase.listEvents(page, size);
        return ResponseEntity.ok(new EventPage(
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                result.items().stream().map(EventsHttpAdapter::toResponse).toList()));
    }

    @Override
    public ResponseEntity<Event> publishEvent(UUID eventId) {
        var event = publishEventUseCase.publish(eventId);
        return ResponseEntity.ok(new Event(event.eventId(), event.name(),
                OffsetDateTime.ofInstant(event.startsAt(), ZoneOffset.UTC),
                new Money(event.price(), Money.CurrencyEnum.fromValue(event.currency())),
                event.capacity(), Event.StatusEnum.fromValue(event.status())));
    }

    private static Event toResponse(BrowseCatalogUseCase.CatalogEvent event) {
        return new Event(event.eventId(), event.name(),
                OffsetDateTime.ofInstant(event.startsAt(), ZoneOffset.UTC),
                new Money(event.price(), Money.CurrencyEnum.fromValue(event.currency())),
                event.capacity(), Event.StatusEnum.fromValue(event.status()));
    }
}
