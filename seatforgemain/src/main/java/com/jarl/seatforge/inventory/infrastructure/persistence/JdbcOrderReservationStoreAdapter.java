package com.jarl.seatforge.inventory.infrastructure.persistence;

import com.jarl.seatforge.inventory.application.port.out.OrderReservationStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public final class JdbcOrderReservationStoreAdapter implements OrderReservationStore {
    private final JdbcTemplate jdbc;

    public JdbcOrderReservationStoreAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<StoredOrderReservation> findForUpdate(UUID reservationId) {
        return jdbc.query(con -> {
            var statement = con.prepareStatement("""
                    select r.reservation_id, r.ticket_id, r.buyer_id, r.status, r.expires_at,
                           t.price_amount, t.currency
                    from reservations r
                    join tickets t on t.ticket_id = r.ticket_id
                    where r.reservation_id = ?
                    for update of r
                    """);
            statement.setObject(1, reservationId);
            return statement;
        }, (rs, row) -> new StoredOrderReservation(
                rs.getObject("reservation_id", UUID.class),
                rs.getObject("ticket_id", UUID.class),
                rs.getString("buyer_id"), rs.getString("status"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getBigDecimal("price_amount"), rs.getString("currency")))
                .stream().findFirst();
    }
}
