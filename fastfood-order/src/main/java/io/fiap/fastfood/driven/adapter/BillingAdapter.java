package io.fiap.fastfood.driven.adapter;

import io.fiap.fastfood.driven.core.domain.billing.mapper.BillingMapper;
import io.fiap.fastfood.driven.core.domain.billing.port.outbound.BillingPort;
import io.fiap.fastfood.driven.core.domain.counter.port.outbound.CounterPort;
import io.fiap.fastfood.driven.core.domain.model.Billing;
import io.fiap.fastfood.driven.core.entity.BillingEntity;
import io.fiap.fastfood.driven.repository.BillingRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
public class BillingAdapter implements BillingPort {

    private final BillingRepository billingRepository;
    private final CounterPort counterPort;
    private final BillingMapper mapper;

    public BillingAdapter(BillingRepository billingRepository,
                          CounterPort counterPort,
                          BillingMapper mapper) {
        this.billingRepository = billingRepository;
        this.counterPort = counterPort;
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public Mono<Billing> openBillingDay() {
        return billingRepository.findByClosedAtNull()
            .next()
            .switchIfEmpty(Mono.defer(() ->
                Mono.just(BillingEntity.BillingEntityBuilder.builder().withOpenAt(LocalDateTime.now()).build())
                    .flatMap(billingRepository::save)))
            .flatMap(billing -> counterPort.resetSequence("order_number")
                .map(__ -> billing))
            .map(mapper::domainFromEntity);
    }

    @Override
    public Mono<Billing> closeBillingDay() {
        return billingRepository.findByClosedAtNull()
            .next()
            .map(billing -> BillingEntity.BillingEntityBuilder.from(billing)
                .withClosedAt(LocalDateTime.now())
                .build())
            .flatMap(billingRepository::save)
            .map(mapper::domainFromEntity);
    }
}
