package com.jarl.seatforge.orders.infrastructure.http;

import com.jarl.seatforge.contract.api.OrdersApi;
import com.jarl.seatforge.contract.model.CreateOrderRequest;
import com.jarl.seatforge.contract.model.Money;
import com.jarl.seatforge.contract.model.Order;
import com.jarl.seatforge.orders.application.port.in.CreateOrderUseCase;
import com.jarl.seatforge.orders.application.port.in.GetOrderUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OrdersHttpAdapter implements OrdersApi {
    private final CreateOrderUseCase createOrders;
    private final GetOrderUseCase getOrders;

    public OrdersHttpAdapter(CreateOrderUseCase createOrders, GetOrderUseCase getOrders) {
        this.createOrders = createOrders;
        this.getOrders = getOrders;
    }

    @Override
    public ResponseEntity<Order> createOrder(UUID idempotencyKey, CreateOrderRequest request) {
        var created = createOrders.create(request.getReservationId(), idempotencyKey);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.orderId()))
                .body(toContract(created));
    }

    @Override
    public ResponseEntity<Order> getOrder(UUID orderId) {
        return ResponseEntity.ok(toContract(getOrders.get(orderId)));
    }

    private static Order toContract(CreateOrderUseCase.OrderResult order) {
        return new Order(order.orderId(), order.reservationId(), order.ticketId(),
                new Money(order.totalAmount(), Money.CurrencyEnum.fromValue(order.currency())),
                Order.StatusEnum.fromValue(order.status()),
                OffsetDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC));
    }
}
