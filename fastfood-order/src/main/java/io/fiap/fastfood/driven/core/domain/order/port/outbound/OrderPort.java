package io.fiap.fastfood.driven.core.domain.order.port.outbound;

import io.fiap.fastfood.driven.core.domain.model.Order;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderPort {

    Mono<Order> createOrder(Order order);

    Mono<Order> updateOrder(String id, String operations);

    Mono<Void> deleteOrder(String id);

    Flux<Order> findAll(Pageable pageable);

    Mono<Order> findById(String id);
}
