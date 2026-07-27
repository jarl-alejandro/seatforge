package com.jarl.seatforge.events.infrastructure;

import com.jarl.seatforge.events.application.port.in.CreateEventUseCase;
import com.jarl.seatforge.events.application.port.in.BrowseCatalogUseCase;
import com.jarl.seatforge.events.application.port.in.PublishEventUseCase;
import com.jarl.seatforge.events.application.port.out.EventIdGenerator;
import com.jarl.seatforge.events.application.port.out.EventStore;
import com.jarl.seatforge.events.application.usecase.CreateEventHandler;
import com.jarl.seatforge.events.application.usecase.BrowseCatalogHandler;
import com.jarl.seatforge.events.application.usecase.PublishEventHandler;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.CreateEventInventory;
import com.jarl.seatforge.inventory.application.port.in.QueryEventInventory;
import com.jarl.seatforge.inventory.application.port.in.PublishEventInventory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class EventsModuleConfiguration {

    @Bean
    Clock seatForgeClock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateEventUseCase createEventUseCase(
            CurrentActor currentActor,
            EventStore eventStore,
            EventIdGenerator eventIdGenerator,
            CreateEventInventory createEventInventory,
            Clock seatForgeClock
    ) {
        return new CreateEventHandler(
                currentActor, eventStore, eventIdGenerator, createEventInventory, seatForgeClock);
    }

    @Bean
    PublishEventUseCase publishEventUseCase(CurrentActor currentActor, EventStore eventStore,
                                            QueryEventInventory inventory, Clock seatForgeClock,
                                            PublishEventInventory publishEventInventory) {
        return new PublishEventHandler(currentActor, eventStore, inventory, seatForgeClock,
                publishEventInventory);
    }

    @Bean
    BrowseCatalogUseCase browseCatalogUseCase(EventStore eventStore,
                                              QueryEventInventory inventory,
                                              Clock seatForgeClock) {
        return new BrowseCatalogHandler(eventStore, inventory, seatForgeClock);
    }
}
