package com.jarl.seatforge.inventory.infrastructure;

import com.jarl.seatforge.inventory.application.port.in.CreateEventInventory;
import com.jarl.seatforge.inventory.application.port.in.QueryEventInventory;
import com.jarl.seatforge.inventory.application.port.in.ReserveTicketUseCase;
import com.jarl.seatforge.inventory.application.port.in.PublishEventInventory;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.out.ReservationStore;
import com.jarl.seatforge.inventory.application.port.out.TicketStore;
import com.jarl.seatforge.inventory.application.usecase.CreateEventInventoryHandler;
import com.jarl.seatforge.inventory.application.usecase.QueryEventInventoryHandler;
import com.jarl.seatforge.inventory.application.usecase.ReserveTicketHandler;
import com.jarl.seatforge.inventory.application.usecase.PublishEventInventoryHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class InventoryModuleConfiguration {

    @Bean
    CreateEventInventory createEventInventory(TicketStore ticketStore) {
        return new CreateEventInventoryHandler(ticketStore);
    }

    @Bean
    QueryEventInventory queryEventInventory(TicketStore ticketStore) {
        return new QueryEventInventoryHandler(ticketStore);
    }

    @Bean
    PublishEventInventory publishEventInventory(TicketStore ticketStore) {
        return new PublishEventInventoryHandler(ticketStore);
    }

    @Bean
    ReserveTicketUseCase reserveTicketUseCase(
            CurrentActor currentActor,
            ReservationStore reservationStore,
            Clock seatForgeClock,
            @Value("${seatforge.inventory.reservation-ttl:PT10M}") Duration reservationTtl
    ) {
        return new ReserveTicketHandler(currentActor, reservationStore,
                seatForgeClock, reservationTtl);
    }
}
