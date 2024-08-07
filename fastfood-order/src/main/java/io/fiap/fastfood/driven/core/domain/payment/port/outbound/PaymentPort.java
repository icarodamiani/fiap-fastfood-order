package io.fiap.fastfood.driven.core.domain.payment.port.outbound;

import io.fiap.fastfood.driven.core.domain.model.Payment;
import reactor.core.publisher.Mono;

public interface PaymentPort {

    Mono<Payment> publishPaymentCommand(Payment payment);

}
