package io.fiap.fastfood.driven.core.service;

import io.fiap.fastfood.driven.core.domain.counter.port.outbound.CounterPort;
import io.fiap.fastfood.driven.core.domain.customer.port.outbound.CustomerPort;
import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.model.Payment;
import io.fiap.fastfood.driven.core.domain.order.port.inbound.OrderUseCase;
import io.fiap.fastfood.driven.core.domain.order.port.outbound.OrderPort;
import io.fiap.fastfood.driven.core.domain.payment.port.outbound.PaymentPort;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.TrackingPort;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.vavr.Function1;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

@Service
public class OrderService implements OrderUseCase {

    private final OrderPort orderPort;
    private final CounterPort counterPort;
    private final PaymentPort paymentPort;
    private final CustomerPort customerPort;
    private final TrackingPort trackingPort;

    public OrderService(OrderPort orderPort,
                        CounterPort counterPort,
                        PaymentPort paymentPort,
                        CustomerPort customerPort, TrackingPort trackingPort) {
        this.orderPort = orderPort;
        this.counterPort = counterPort;
        this.paymentPort = paymentPort;
        this.customerPort = customerPort;
        this.trackingPort = trackingPort;
    }

    @Transactional
    @Override
    public Mono<Order> create(Order order) {
        return Mono.just(order)
            .switchIfEmpty(Mono.defer(() -> Mono.error(new BadRequestException())))
            .zipWith(counterPort.nextSequence("order_number_seq"))
            .flatMap(t ->
                orderPort.publishOrderCommand(
                    Order.OrderBuilder
                        .from(order)
                        .withNumber(t.getT2().toString())
                        .build())
            );
    }

    @Override
    public Flux<Message> handleEvent() {
        return orderPort.readOrder(handle());
    }

    private Function1<Order, Mono<Order>> handle() {
        return order -> Mono.just(order)
            .flatMap(orderPort::createOrder)
            .doOnNext(o -> customerPort.publishCustomerCommand(order.customer()))
            .flatMap(o ->
                Mono.justOrEmpty(toTracking().apply(o))
                    .flatMap(trackingPort::create)
                    .map(tracking -> o)
            )
            .flatMap(o ->
                paymentPort.publishPaymentCommand(Payment.PaymentBuilder.from(order.payment())
                        .withOrderId(o.id())
                        .withDateTime(LocalDateTime.now())
                        .build())
                    .map(payment -> o)
            );
    }

    private Function1<Order, OrderTracking> toTracking() {
        return order -> OrderTracking.OrderTrackingBuilder.builder()
            .withOrderId(order.id())
            .withOrderStatus("WAITING_PAYMENT")
            .withOrderStatusValue("1")
            .build();
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
