package io.fiap.fastfood.driven.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fiap.fastfood.driven.core.domain.model.Payment;
import io.fiap.fastfood.driven.core.domain.payment.port.outbound.PaymentPort;
import io.fiap.fastfood.driven.core.messaging.MessagingPort;
import io.vavr.CheckedFunction1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentAdapter implements PaymentPort {

    private final MessagingPort messagingPort;
    private final ObjectMapper objectMapper;
    private final String queue;

    public PaymentAdapter(MessagingPort messagingPort, ObjectMapper objectMapper,
                          @Value("${aws.sqs.payment.queue}") String queue) {
        this.messagingPort = messagingPort;
        this.objectMapper = objectMapper;
        this.queue = queue;
    }

    public Mono<Payment> publishPaymentCommand(Payment payment) {
        return messagingPort.send(queue, payment, serializePayload());
    }

    private <T> CheckedFunction1<T, String> serializePayload() {
        return objectMapper::writeValueAsString;
    }
}
