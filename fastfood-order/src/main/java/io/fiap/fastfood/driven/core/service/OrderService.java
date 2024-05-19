package io.fiap.fastfood.driven.core.service;

import io.fiap.fastfood.driven.core.domain.counter.port.outbound.CounterPort;
import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.domain.model.Payment;
import io.fiap.fastfood.driven.core.domain.order.port.inbound.OrderUseCase;
import io.fiap.fastfood.driven.core.domain.order.port.outbound.OrderPort;
import io.fiap.fastfood.driven.core.domain.payment.port.outbound.PaymentPort;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService implements OrderUseCase {

    private final OrderPort orderPort;
    private final CounterPort counterPort;
    private final PaymentPort paymentPort;

    public OrderService(OrderPort orderPort, CounterPort counterPort, PaymentPort paymentPort) {
        this.orderPort = orderPort;
        this.counterPort = counterPort;
        this.paymentPort = paymentPort;
    }

    @Transactional
    @Override
    public Mono<Order> create(Order order) {
        return Mono.just(order)
            .switchIfEmpty(Mono.defer(() -> Mono.error(new BadRequestException())))
            .zipWith(counterPort.nextSequence("order_number_seq"))
            .flatMap(t ->
                orderPort.createOrder(
                    Order.OrderBuilder
                        .from(order)
                        .withNumber(t.getT2().toString())
                        .build())
            )
            .flatMap(o ->
                paymentPort.createPayment(Payment.PaymentBuilder.from(order.payment())
                        .withOrderId(o.id())
                        .withDateTime(LocalDateTime.now())
                        .build())
                    .map(payment -> o)
            );
    }

    @Override
    public Flux<Order> findAll(Pageable pageable) {
        return orderPort.findAll(pageable);
    }

    @Override
    public Mono<Order> findById(String id) {
        return orderPort.findById(id);
    }

    @Override
    public Mono<Void> delete(String id) {
        return Mono.just(id)
            .switchIfEmpty(Mono.defer(() -> Mono.error(new BadRequestException())))
            .flatMap(orderPort::deleteOrder);
    }

    @Override
    public Mono<Order> update(String id, String operations) {
        return orderPort.updateOrder(id, operations);
    }

}
