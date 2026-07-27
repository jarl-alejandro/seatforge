alter table tickets add column event_published boolean not null default false;

update tickets set event_published = true
where event_id in (select event_id from events where status = 'PUBLISHED');
