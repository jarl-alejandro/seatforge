package com.jarl.seatforge.identity.infrastructure.security;

import java.util.Set;

final class SeatForgePermissions {

    static final String CREATE_EVENTS = "create:events";
    static final String PUBLISH_EVENTS = "publish:events";
    static final String RESERVE_TICKETS = "reserve:tickets";
    static final String CREATE_ORDERS = "create:orders";
    static final String READ_ORDERS = "read:orders";
    static final String PAY_ORDERS = "pay:orders";

    static final String ROLE_BUYER = "ROLE_BUYER";
    static final String ROLE_ORGANIZER = "ROLE_ORGANIZER";

    static final Set<String> BUYER = Set.of(
            RESERVE_TICKETS,
            CREATE_ORDERS,
            READ_ORDERS,
            PAY_ORDERS
    );

    static final Set<String> ORGANIZER = Set.of(CREATE_EVENTS, PUBLISH_EVENTS);

    static final Set<String> ALL = Set.of(
            CREATE_EVENTS,
            PUBLISH_EVENTS,
            RESERVE_TICKETS,
            CREATE_ORDERS,
            READ_ORDERS,
            PAY_ORDERS
    );

    private SeatForgePermissions() {
    }
}
