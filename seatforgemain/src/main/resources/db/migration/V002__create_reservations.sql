create table reservations (
    reservation_id uuid primary key,
    ticket_id uuid not null references tickets(ticket_id),
    buyer_id varchar(255) not null,
    status varchar(20) not null check (status in ('ACTIVE', 'EXPIRED')),
    expires_at timestamp with time zone not null,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    constraint uk_reservations_idempotency_key unique (idempotency_key)
);

create index ix_reservations_ticket_id on reservations(ticket_id);
create index ix_reservations_active_expiry on reservations(expires_at)
    where status = 'ACTIVE';
