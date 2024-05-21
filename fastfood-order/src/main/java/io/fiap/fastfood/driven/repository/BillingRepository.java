package io.fiap.fastfood.driven.repository;

import io.fiap.fastfood.driven.core.entity.BillingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface BillingRepository extends ReactiveCrudRepository<BillingEntity, String> {
    Flux<BillingEntity> findByClosedAtNull();

    Flux<BillingEntity> findByIdNotNull(Pageable pageable);
}
