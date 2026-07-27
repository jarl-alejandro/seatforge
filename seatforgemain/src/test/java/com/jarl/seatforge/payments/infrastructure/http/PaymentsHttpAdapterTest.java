package com.jarl.seatforge.payments.infrastructure.http;

import com.jarl.seatforge.contract.model.SimulatePaymentRequest;
import com.jarl.seatforge.payments.application.port.in.SimulatePaymentUseCase;
import com.jarl.seatforge.payments.domain.PaymentScenario;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentsHttpAdapterTest {
 @Test void delegates_generated_contract_and_maps_result(){UUID order=UUID.randomUUID(),key=UUID.randomUUID(),payment=UUID.randomUUID();var useCase=new CapturingUseCase(new SimulatePaymentUseCase.PaymentResult(payment,order,"APPROVED","CONFIRMED","SOLD",Instant.EPOCH));var response=new PaymentsHttpAdapter(useCase).simulatePayment(order,key,new SimulatePaymentRequest(SimulatePaymentRequest.ScenarioEnum.APPROVED).delayMs(7));assertThat(response.getStatusCode().value()).isEqualTo(200);assertThat(response.getBody().getPaymentId()).isEqualTo(payment);assertThat(response.getBody().getTicketStatus().getValue()).isEqualTo("SOLD");assertThat(useCase.delay).isEqualTo(7);assertThat(useCase.scenario).isEqualTo(PaymentScenario.APPROVED);}
 static final class CapturingUseCase implements SimulatePaymentUseCase{private final PaymentResult answer;int delay;PaymentScenario scenario;CapturingUseCase(PaymentResult answer){this.answer=answer;}public PaymentResult simulate(UUID o,UUID k,PaymentScenario s,int d){scenario=s;delay=d;return answer;}}
}
