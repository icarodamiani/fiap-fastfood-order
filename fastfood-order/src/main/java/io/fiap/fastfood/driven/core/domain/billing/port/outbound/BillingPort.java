package io.fiap.fastfood.driven.core.domain.billing.port.outbound;

import io.fiap.fastfood.driven.core.domain.model.Billing;
import reactor.core.publisher.Mono;

public interface BillingPort {
    Mono<Billing> openBillingDay();

    Mono<Billing> closeBillingDay();

}
