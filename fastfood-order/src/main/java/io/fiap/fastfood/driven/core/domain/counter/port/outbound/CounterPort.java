package io.fiap.fastfood.driven.core.domain.counter.port.outbound;

import reactor.core.publisher.Mono;

public interface CounterPort {
    Mono<Long> nextSequence(String name);

    Mono<Long> resetSequence(String name);

}
