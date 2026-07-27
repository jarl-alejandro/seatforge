package com.jarl.seatforge.payments.infrastructure.persistence;

import com.jarl.seatforge.payments.application.port.in.PaymentConflictException;
import com.jarl.seatforge.payments.application.port.out.PaymentStore;
import com.jarl.seatforge.payments.domain.Payment;
import com.jarl.seatforge.payments.domain.PaymentScenario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Component
public final class JdbcPaymentStoreAdapter implements PaymentStore {
    private static final long LOCK_NAMESPACE = 0x5041594d454e54L;
    private final JdbcTemplate jdbc;
    public JdbcPaymentStoreAdapter(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void lockIdempotencyKey(UUID key) {
        long lock = key.getMostSignificantBits() ^ key.getLeastSignificantBits() ^ LOCK_NAMESPACE;
        jdbc.queryForObject("select pg_advisory_xact_lock(?)", Object.class, lock);
    }
    @Override public Optional<StoredPayment> findByIdempotencyKey(UUID key) { return payment("where idempotency_key = ?", key); }
    @Override public Optional<StoredPayment> findByOrderId(UUID orderId) { return payment("where order_id = ?", orderId); }
    private Optional<StoredPayment> payment(String where, UUID value) {
        return jdbc.query("select * from payments " + where, (rs,row) -> new StoredPayment(
                new Payment(rs.getObject("payment_id",UUID.class),rs.getObject("order_id",UUID.class),
                        PaymentScenario.valueOf(rs.getString("result")),rs.getString("order_status"),
                        rs.getString("ticket_status"),rs.getTimestamp("processed_at").toInstant()),
                rs.getString("request_hash")), value).stream().findFirst();
    }
    @Override public Optional<OrderForPayment> findOwnedOrderForUpdate(UUID orderId, String buyerId) {
        return jdbc.query("""
                select o.order_id,o.ticket_id,o.status,t.status ticket_status
                from purchase_orders o join tickets t on t.ticket_id=o.ticket_id
                where o.order_id=? and o.buyer_id=? for update of o,t
                """, (rs,row)->new OrderForPayment(rs.getObject("order_id",UUID.class),
                rs.getObject("ticket_id",UUID.class),rs.getString("status"),rs.getString("ticket_status")),
                orderId,buyerId).stream().findFirst();
    }
    @Override public void apply(Payment p, UUID key, String hash) {
        int order = jdbc.update("update purchase_orders set status=? where order_id=? and status='PENDING'", p.orderStatus(),p.orderId());
        if (order != 1) throw new PaymentConflictException("Order changed concurrently");
        String expectedTicket = p.result()==PaymentScenario.APPROVED ? "SOLD" : "AVAILABLE";
        int ticket = jdbc.update("""
                update tickets set status=? where ticket_id=(select ticket_id from purchase_orders where order_id=?)
                and status='RESERVED'
                """, expectedTicket,p.orderId());
        if (ticket != 1) throw new PaymentConflictException("Ticket changed concurrently");
        jdbc.update("""
                insert into payments(payment_id,order_id,idempotency_key,request_hash,result,order_status,ticket_status,processed_at)
                values (?,?,?,?,?,?,?,?)
                """,p.id(),p.orderId(),key,hash,p.result().name(),p.orderStatus(),p.ticketStatus(),Timestamp.from(p.processedAt()));
    }
}
