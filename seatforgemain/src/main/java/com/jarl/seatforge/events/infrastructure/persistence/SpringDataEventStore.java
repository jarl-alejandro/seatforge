package com.jarl.seatforge.events.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jarl.seatforge.events.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

interface SpringDataEventStore extends JpaRepository<EventJpaEntity, UUID> {
    Page<EventJpaEntity> findByStatusAndStartsAtAfter(
            EventStatus status, Instant startsAt, Pageable pageable);
}
