package com.jarl.seatforge.events.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataEventStore extends JpaRepository<EventJpaEntity, UUID> {
}
