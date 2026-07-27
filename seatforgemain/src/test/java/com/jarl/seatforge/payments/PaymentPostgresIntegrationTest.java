package com.jarl.seatforge.payments;

import com.jarl.seatforge.identity.application.port.in.*;
import com.jarl.seatforge.payments.application.port.in.*;
import com.jarl.seatforge.payments.domain.PaymentScenario;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={"spring.flyway.enabled=true","spring.jpa.hibernate.ddl-auto=validate","seatforge.payments.max-delay-ms=100"})
class PaymentPostgresIntegrationTest {
 @Container @ServiceConnection static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17-alpine");
 @Autowired SimulatePaymentUseCase payments; @Autowired JdbcTemplate jdbc; @Autowired Actor actor;
 @BeforeEach void setup(){jdbc.execute("drop trigger if exists fail_ticket_update on tickets");jdbc.execute("drop function if exists fail_ticket_update()");jdbc.update("delete from payments");jdbc.update("delete from purchase_orders");jdbc.update("delete from reservations");jdbc.update("delete from tickets");jdbc.update("delete from events");actor.set("buyer");}
 @AfterEach void cleanup(){actor.clear();}

 @Test void approves_atomically_and_replays_same_or_terminal_requests(){var ids=order("buyer");UUID key=UUID.randomUUID();var first=payments.simulate(ids.order,key,PaymentScenario.APPROVED,0);assertThat(first.orderStatus()).isEqualTo("CONFIRMED");assertThat(first.ticketStatus()).isEqualTo("SOLD");assertThat(payments.simulate(ids.order,key,PaymentScenario.APPROVED,0)).isEqualTo(first);assertThat(payments.simulate(ids.order,UUID.randomUUID(),PaymentScenario.DECLINED,0)).isEqualTo(first);assertStates(ids,"CONFIRMED","SOLD",1);}
 @Test void declines_and_releases_without_ever_selling(){var ids=order("buyer");var out=payments.simulate(ids.order,UUID.randomUUID(),PaymentScenario.DECLINED,0);assertThat(out.result()).isEqualTo("DECLINED");assertStates(ids,"DECLINED","AVAILABLE",1);assertThat(payments.simulate(ids.order,UUID.randomUUID(),PaymentScenario.APPROVED,0)).isEqualTo(out);assertStates(ids,"DECLINED","AVAILABLE",1);}
 @Test void timeout_is_delayed_configurable_and_has_no_mutation(){var ids=order("buyer");long start=System.nanoTime();assertThatThrownBy(()->payments.simulate(ids.order,UUID.randomUUID(),PaymentScenario.TIMEOUT,35)).isInstanceOf(SimulatedPaymentTimeoutException.class);assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)).isGreaterThanOrEqualTo(30);assertStates(ids,"PENDING","RESERVED",0);assertThatThrownBy(()->payments.simulate(ids.order,UUID.randomUUID(),PaymentScenario.TIMEOUT,101)).isInstanceOf(IllegalArgumentException.class);}
 @Test void rejects_reused_key_with_different_payload_and_hides_other_buyers(){var a=order("buyer");var b=order("buyer");UUID key=UUID.randomUUID();payments.simulate(a.order,key,PaymentScenario.APPROVED,0);assertThatThrownBy(()->payments.simulate(b.order,key,PaymentScenario.DECLINED,0)).isInstanceOf(PaymentIdempotencyConflictException.class);actor.set("other");assertThatThrownBy(()->payments.simulate(b.order,UUID.randomUUID(),PaymentScenario.APPROVED,0)).isInstanceOf(PaymentNotFoundException.class);}
 @Test void failure_between_order_and_ticket_changes_rolls_back_everything(){var ids=order("buyer");jdbc.execute("create function fail_ticket_update() returns trigger language plpgsql as $$ begin raise exception 'forced'; end $$");jdbc.execute("create trigger fail_ticket_update before update on tickets for each row execute function fail_ticket_update()");assertThatThrownBy(()->payments.simulate(ids.order,UUID.randomUUID(),PaymentScenario.APPROVED,0)).hasMessageContaining("forced");jdbc.execute("drop trigger fail_ticket_update on tickets");jdbc.execute("drop function fail_ticket_update()");assertStates(ids,"PENDING","RESERVED",0);}
 @Test void twenty_concurrent_equal_payments_produce_one_terminal_effect() throws Exception {var ids=order("buyer");UUID key=UUID.randomUUID();try(var executor=Executors.newFixedThreadPool(20)){var ready=new CountDownLatch(20);var start=new CountDownLatch(1);List<Future<SimulatePaymentUseCase.PaymentResult>> fs=new ArrayList<>();for(int i=0;i<20;i++)fs.add(executor.submit(()->{actor.set("buyer");ready.countDown();start.await();try{return payments.simulate(ids.order,key,PaymentScenario.APPROVED,0);}finally{actor.clear();}}));ready.await();start.countDown();Set<UUID> paymentIds=new HashSet<>();for(var f:fs)paymentIds.add(f.get().paymentId());assertThat(paymentIds).hasSize(1);}assertStates(ids,"CONFIRMED","SOLD",1);}

 private Ids order(String buyer){UUID event=UUID.randomUUID(),ticket=UUID.randomUUID(),reservation=UUID.randomUUID(),order=UUID.randomUUID();jdbc.update("insert into events(event_id,owner_id,name,starts_at,price_amount,currency,capacity,status) values(?,'org','Concert',?,25,'USD',1,'PUBLISHED')",event,Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")));jdbc.update("insert into tickets(ticket_id,event_id,ticket_number,status,price_amount,currency,event_published) values(?,?,1,'RESERVED',25,'USD',true)",ticket,event);jdbc.update("insert into reservations(reservation_id,ticket_id,buyer_id,status,expires_at,idempotency_key,request_hash) values(?,?,?,'ACTIVE',?,?,?)",reservation,ticket,buyer,Timestamp.from(Instant.now().plusSeconds(300)),UUID.randomUUID(),"a".repeat(64));jdbc.update("insert into purchase_orders(order_id,reservation_id,ticket_id,buyer_id,total_amount,currency,status,created_at,idempotency_key,request_hash) values(?,?,?,?,25,'USD','PENDING',?,?,?)",order,reservation,ticket,buyer,Timestamp.from(Instant.now()),UUID.randomUUID(),"b".repeat(64));return new Ids(order,ticket);}
 private void assertStates(Ids ids,String order,String ticket,int count){assertThat(jdbc.queryForObject("select status from purchase_orders where order_id=?",String.class,ids.order)).isEqualTo(order);assertThat(jdbc.queryForObject("select status from tickets where ticket_id=?",String.class,ids.ticket)).isEqualTo(ticket);assertThat(jdbc.queryForObject("select count(*) from payments where order_id=?",Integer.class,ids.order)).isEqualTo(count);}
 record Ids(UUID order,UUID ticket){}
 @TestConfiguration(proxyBeanMethods=false) static class Config{@Bean @Primary Actor actor(){return new Actor();}}
 static final class Actor implements CurrentActor {private final ThreadLocal<String> id=new ThreadLocal<>();void set(String s){id.set(s);}void clear(){id.remove();}@Override public AuthenticatedActor get(){return new AuthenticatedActor(new ActorId(id.get()),Set.of(ActorRole.BUYER),Set.of("pay:orders"));}}
}
