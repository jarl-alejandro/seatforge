package com.jarl.seatforge.payments.application.usecase;

import com.jarl.seatforge.identity.application.port.in.*;
import com.jarl.seatforge.payments.application.port.in.*;
import com.jarl.seatforge.payments.application.port.out.PaymentStore;
import com.jarl.seatforge.payments.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SimulatePaymentHandlerTest {
 private static final UUID ORDER=UUID.randomUUID(),TICKET=UUID.randomUUID();
 private final Store store=new Store();
 private final SimulatePaymentHandler handler=new SimulatePaymentHandler(()->new AuthenticatedActor(new ActorId("buyer"),Set.of(ActorRole.BUYER),Set.of()),store,Clock.fixed(Instant.EPOCH,ZoneOffset.UTC),30_000);
 @Test void approval_applies_one_terminal_effect(){var r=handler.simulate(ORDER,UUID.randomUUID(),PaymentScenario.APPROVED,0);assertThat(r.result()).isEqualTo("APPROVED");assertThat(store.applied.ticketStatus()).isEqualTo("SOLD");}
 @Test void timeout_never_applies(){assertThatThrownBy(()->handler.simulate(ORDER,UUID.randomUUID(),PaymentScenario.TIMEOUT,0)).isInstanceOf(SimulatedPaymentTimeoutException.class);assertThat(store.applied).isNull();}
 @Test void terminal_order_replays_original_result_even_with_new_key(){store.terminal=new Payment(UUID.randomUUID(),ORDER,PaymentScenario.DECLINED,"DECLINED","AVAILABLE",Instant.EPOCH);var r=handler.simulate(ORDER,UUID.randomUUID(),PaymentScenario.APPROVED,0);assertThat(r.result()).isEqualTo("DECLINED");assertThat(store.applied).isNull();}
 @Test void reused_key_with_other_payload_conflicts(){UUID key=UUID.randomUUID();handler.simulate(ORDER,key,PaymentScenario.APPROVED,0);assertThatThrownBy(()->handler.simulate(UUID.randomUUID(),key,PaymentScenario.APPROVED,0)).isInstanceOf(PaymentIdempotencyConflictException.class);}
 static final class Store implements PaymentStore{Map<UUID,StoredPayment> keys=new HashMap<>();Payment terminal,applied;public void lockIdempotencyKey(UUID k){}public Optional<StoredPayment> findByIdempotencyKey(UUID k){return Optional.ofNullable(keys.get(k));}public Optional<StoredPayment> findByOrderId(UUID o){return Optional.ofNullable(terminal).map(p->new StoredPayment(p,"x"));}public Optional<OrderForPayment> findOwnedOrderForUpdate(UUID o,String b){return Optional.of(new OrderForPayment(o,TICKET,"PENDING","RESERVED"));}public void apply(Payment p,UUID k,String h){applied=p;keys.put(k,new StoredPayment(p,h));}}
}
