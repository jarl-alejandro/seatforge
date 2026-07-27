package com.jarl.seatforge.payments.infrastructure.http;

import com.jarl.seatforge.contract.api.PaymentsApi;
import com.jarl.seatforge.contract.model.PaymentResult;
import com.jarl.seatforge.contract.model.SimulatePaymentRequest;
import com.jarl.seatforge.payments.application.port.in.SimulatePaymentUseCase;
import com.jarl.seatforge.payments.domain.PaymentScenario;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class PaymentsHttpAdapter implements PaymentsApi {
    private final SimulatePaymentUseCase payments;
    public PaymentsHttpAdapter(SimulatePaymentUseCase payments) { this.payments=payments; }
    @Override public ResponseEntity<PaymentResult> simulatePayment(UUID orderId, UUID key, SimulatePaymentRequest request) {
        int delay = request.getDelayMs()==null ? 0 : request.getDelayMs();
        var result=payments.simulate(orderId,key,PaymentScenario.valueOf(request.getScenario().getValue()),delay);
        return ResponseEntity.ok(new PaymentResult(result.paymentId(),result.orderId(),
                PaymentResult.ResultEnum.fromValue(result.result()),PaymentResult.OrderStatusEnum.fromValue(result.orderStatus()),
                PaymentResult.TicketStatusEnum.fromValue(result.ticketStatus()),
                OffsetDateTime.ofInstant(result.processedAt(), ZoneOffset.UTC)));
    }
}
