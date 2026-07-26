create table events (
    event_id uuid primary key,
    owner_id varchar(255) not null,
    name varchar(120) not null,
    starts_at timestamp with time zone not null,
    price_amount numeric(19, 2) not null check (price_amount > 0),
    currency varchar(3) not null check (currency = 'USD'),
    capacity integer not null check (capacity between 1 and 100000),
    status varchar(20) not null check (status in ('DRAFT', 'PUBLISHED'))
);

create table tickets (
    ticket_id uuid primary key,
    event_id uuid not null references events(event_id) on delete cascade,
    ticket_number integer not null check (ticket_number > 0),
    status varchar(20) not null check (status in ('AVAILABLE', 'RESERVED', 'SOLD')),
    price_amount numeric(19, 2) not null check (price_amount > 0),
    currency varchar(3) not null check (currency = 'USD'),
    constraint uk_tickets_event_number unique (event_id, ticket_number)
);

create index ix_tickets_event_id on tickets(event_id);
