package com.jarl.seatforge.inventory.infrastructure;

import com.jarl.seatforge.inventory.application.port.in.CreateEventInventory;
import com.jarl.seatforge.inventory.application.port.out.TicketStore;
import com.jarl.seatforge.inventory.application.usecase.CreateEventInventoryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InventoryModuleConfiguration {

    @Bean
    CreateEventInventory createEventInventory(TicketStore ticketStore) {
        return new CreateEventInventoryHandler(ticketStore);
    }
}
