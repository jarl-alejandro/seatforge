create table purchase_orders (
    order_id uuid primary key,
    reservation_id uuid not null references reservations(reservation_id),
    ticket_id uuid not null references tickets(ticket_id),
    buyer_id varchar(255) not null,
    total_amount numeric(19, 2) not null check (total_amount > 0),
    currency varchar(3) not null check (currency = 'USD'),
    status varchar(20) not null check (status in ('PENDING', 'CONFIRMED', 'DECLINED')),
    created_at timestamp with time zone not null,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    constraint uk_purchase_orders_reservation unique (reservation_id),
    constraint uk_purchase_orders_idempotency_key unique (idempotency_key)
);

create index ix_purchase_orders_buyer_id on purchase_orders(buyer_id);
