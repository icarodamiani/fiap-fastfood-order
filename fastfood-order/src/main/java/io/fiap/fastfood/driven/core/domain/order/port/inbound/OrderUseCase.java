package io.fiap.fastfood.driven.core.domain.order.port.inbound;

import io.fiap.fastfood.driven.core.domain.model.Order;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderUseCase {
    Mono<Order> create(Order value);

    Flux<Order> findAll(Pageable pageable);

    Mono<Order> findById(String id);

    Mono<Void> delete(String id);

    Mono<Order> update(String id, String operations);
}
