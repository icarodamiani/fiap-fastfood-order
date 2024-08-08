package io.fiap.fastfood.driven.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.fiap.fastfood.driven.core.domain.customer.port.outbound.CustomerPort;
import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.domain.model.OrderTracking;
import io.fiap.fastfood.driven.core.domain.model.Payment;
import io.fiap.fastfood.driven.core.domain.order.mapper.OrderMapper;
import io.fiap.fastfood.driven.core.domain.order.port.outbound.OrderPort;
import io.fiap.fastfood.driven.core.domain.payment.port.outbound.PaymentPort;
import io.fiap.fastfood.driven.core.domain.tracking.port.outbound.TrackingPort;
import io.fiap.fastfood.driven.core.entity.OrderEntity;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.fiap.fastfood.driven.core.messaging.MessagingPort;
import io.fiap.fastfood.driven.repository.OrderRepository;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

@Service
public class OrderAdapter implements OrderPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderAdapter.class);

    private final MessagingPort messagingPort;
    private final PaymentPort paymentPort;
    private final CustomerPort customerPort;
    private final TrackingPort trackingPort;
    private final OrderRepository orderRepository;
    private final OrderMapper mapper;
    private final ObjectMapper objectMapper;
    private final String queue;

    public OrderAdapter(MessagingPort messagingPort,
                        PaymentPort paymentPort,
                        CustomerPort customerPort,
                        TrackingPort trackingPort,
                        @Value("${aws.sqs.order.queue}") String queue,
                        OrderRepository orderRepository,
                        OrderMapper mapper,
                        ObjectMapper objectMapper) {
        this.messagingPort = messagingPort;
        this.paymentPort = paymentPort;
        this.customerPort = customerPort;
        this.trackingPort = trackingPort;
        this.queue = queue;
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Order> publishOrderCommand(Order order) {
        return messagingPort.send(queue, order, serializePayload());
    }

    @Override
    public Flux<Message> readOrder() {
        return messagingPort.read(queue)
            .flatMap(message ->
                Mono.fromCallable(() -> readEvent().unchecked().apply(message))
                    .flatMap(this::createOrder)
                    .flatMap(o -> customerPort.publishCustomerCommand(o.customer())
                        .map(customer -> o)
                        .onErrorResume(throwable -> Mono.defer(() -> Mono.just(o))))
                    .flatMap(o -> trackingPort.publishTracking(toTracking().apply(o))
                        .map(tracking -> o)
                    )
                    .flatMap(o -> paymentPort.publishPaymentCommand(Payment.PaymentBuilder.from(o.payment())
                            .withOrderNumber(o.number())
                            .withDateTime(LocalDateTime.now())
                            .build())
                        .map(payment -> o)
                    )
                    .doOnSubscribe(subscription -> LOGGER.info("Handle begin. {}", message))
                    .doOnSuccess(m -> LOGGER.info("Handle succeeded. {}", message))
                    .map(unused -> message)
            )
            .flatMap(message -> messagingPort.ack(queue, message));
    }


    private Function<Order, OrderTracking> toTracking() {
        return order -> OrderTracking.OrderTrackingBuilder.builder()
            .withOrderNumber(order.number())
            .withOrderStatus("WAITING_PAYMENT")
            .withOrderStatusValue("1")
            .build();
    }

    @Override
    public Flux<Message> readOrderCancel() {
        return messagingPort.read(queue);
    }

    private CheckedFunction1<Message, Order> readEvent() {
        return message -> objectMapper.readValue(message.body(), Order.class);
    }

    @Override
    public Mono<Order> createOrder(Order order) {
        return orderRepository.save(mapper.entityFromDomain(order))
            .map(mapper::domainFromEntity)
            .map(unused -> order)
            .switchIfEmpty(Mono.defer(() -> Mono.just(order)
                .doOnSubscribe(subscription -> LOGGER.info("EMPTY RESPONSE FROM MONGO."))));
    }

    @Override
    public Mono<Void> deleteOrder(String id) {
        return orderRepository.deleteById(id);
    }

    @Override
    public Mono<Order> updateOrder(String id, String operations) {
        return orderRepository.findById(id)
            .map(order -> applyPatch().unchecked().apply(order, operations))
            .flatMap(orderRepository::save)
            .map(mapper::domainFromEntity)
            .onErrorMap(JsonPatchException.class::isInstance, BadRequestException::new);
    }

    private CheckedFunction2<OrderEntity, String, OrderEntity> applyPatch() {
        return (order, operations) -> {
            var patch = readOperations()
                .unchecked()
                .apply(operations);

            var patched = patch.apply(objectMapper.convertValue(order, JsonNode.class));

            return objectMapper.treeToValue(patched, OrderEntity.class);
        };
    }

    private CheckedFunction1<String, JsonPatch> readOperations() {
        return operations -> {
            final InputStream in = new ByteArrayInputStream(operations.getBytes());
            return objectMapper.readValue(in, JsonPatch.class);
        };
    }

    @Override
    public Flux<Order> findAll(Pageable pageable) {
        return orderRepository.findByIdNotNull(pageable)
            .map(mapper::domainFromEntity);
    }

    @Override
    public Mono<Order> findById(String id) {
        return orderRepository.findById(id)
            .map(mapper::domainFromEntity);
    }

    private <T> CheckedFunction1<T, String> serializePayload() {
        return objectMapper::writeValueAsString;
    }

}
