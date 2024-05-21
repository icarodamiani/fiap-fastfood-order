package io.fiap.fastfood.driven.core.domain.billing.port.inbound;

import io.fiap.fastfood.driven.core.domain.model.Billing;
import reactor.core.publisher.Mono;

public interface BillingUseCase {
    Mono<Billing> open();
    Mono<Billing> close();
}
