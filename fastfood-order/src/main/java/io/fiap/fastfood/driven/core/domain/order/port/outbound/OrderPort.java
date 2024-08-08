package io.fiap.fastfood.driven.core.domain.order.port.outbound;

import io.fiap.fastfood.driven.core.domain.model.Order;
import io.vavr.CheckedFunction1;
import java.util.function.Function;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

public interface OrderPort {

    Mono<Order> publishOrderCommand(Order order);

    Flux<Message> readOrder();

    Flux<Message> readOrderCancel();

    Mono<Order> createOrder(Order order);

    Mono<Order> updateOrder(String id, String operations);

    Mono<Void> deleteOrder(String id);

    Flux<Order> findAll(Pageable pageable);

    Mono<Order> findById(String id);
}
