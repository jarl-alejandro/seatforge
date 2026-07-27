package com.jarl.seatforge.inventory.infrastructure.persistence;

import com.jarl.seatforge.inventory.application.port.out.ExpiredReservationStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JdbcExpiredReservationStoreAdapter implements ExpiredReservationStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcExpiredReservationStoreAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public BatchOutcome releaseExpired(Instant expiresOnOrBefore, int batchSize) {
        List<Candidate> candidates = jdbcTemplate.query("""
                        select r.reservation_id, r.ticket_id
                          from reservations r join tickets t on t.ticket_id = r.ticket_id
                         where r.status = 'ACTIVE' and r.expires_at <= ? and t.status = 'RESERVED'
                         order by r.expires_at, r.reservation_id
                         limit ? for update of r, t skip locked
                        """,
                (rs, rowNum) -> new Candidate(rs.getObject("reservation_id", UUID.class),
                        rs.getObject("ticket_id", UUID.class)),
                Timestamp.from(expiresOnOrBefore), batchSize);

        int released = 0;
        for (Candidate candidate : candidates) {
            int expired = jdbcTemplate.update("""
                            update reservations set status = 'EXPIRED'
                             where reservation_id = ? and status = 'ACTIVE' and expires_at <= ?
                            """, candidate.reservationId(), Timestamp.from(expiresOnOrBefore));
            if (expired == 1) {
                released += jdbcTemplate.update("""
                                update tickets set status = 'AVAILABLE'
                                 where ticket_id = ? and status = 'RESERVED'
                                """, candidate.ticketId());
            }
        }
        return new BatchOutcome(candidates.size(), released, candidates.size() - released);
    }

    private record Candidate(UUID reservationId, UUID ticketId) {}
}
