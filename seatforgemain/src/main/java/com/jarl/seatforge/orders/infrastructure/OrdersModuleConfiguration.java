package com.jarl.seatforge.orders.infrastructure;

import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.inventory.application.port.in.GetReservationForOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.CreateOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.GetOrderUseCase;
import com.jarl.seatforge.orders.application.port.out.OrderStore;
import com.jarl.seatforge.orders.application.usecase.CreateOrderHandler;
import com.jarl.seatforge.orders.application.usecase.GetOrderHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class OrdersModuleConfiguration {
    @Bean
    CreateOrderUseCase createOrderUseCase(CurrentActor actor,
                                           GetReservationForOrderUseCase reservations,
                                           OrderStore orders, Clock seatForgeClock) {
        return new CreateOrderHandler(actor, reservations, orders, seatForgeClock);
    }

    @Bean
    GetOrderUseCase getOrderUseCase(CurrentActor actor, OrderStore orders) {
        return new GetOrderHandler(actor, orders);
    }
}
