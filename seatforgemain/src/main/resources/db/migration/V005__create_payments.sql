create table payments (
    payment_id uuid primary key,
    order_id uuid not null references purchase_orders(order_id),
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    result varchar(20) not null check (result in ('APPROVED', 'DECLINED')),
    order_status varchar(20) not null check (order_status in ('CONFIRMED', 'DECLINED')),
    ticket_status varchar(20) not null check (ticket_status in ('AVAILABLE', 'SOLD')),
    processed_at timestamp with time zone not null,
    constraint uk_payments_order unique (order_id),
    constraint uk_payments_idempotency_key unique (idempotency_key)
);

create index ix_payments_order_id on payments(order_id);
