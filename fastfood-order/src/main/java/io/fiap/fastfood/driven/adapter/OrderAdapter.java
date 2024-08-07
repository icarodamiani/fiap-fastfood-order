package io.fiap.fastfood.driven.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.domain.order.mapper.OrderMapper;
import io.fiap.fastfood.driven.core.domain.order.port.outbound.OrderPort;
import io.fiap.fastfood.driven.core.entity.OrderEntity;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.fiap.fastfood.driven.core.messaging.MessagingPort;
import io.fiap.fastfood.driven.repository.OrderRepository;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import io.vavr.Function1;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class OrderAdapter implements OrderPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderAdapter.class);

    private final MessagingPort messagingPort;
    private final OrderRepository orderRepository;
    private final OrderMapper mapper;
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsClient;
    private final String queue;

    public OrderAdapter(MessagingPort messagingPort,
                        SqsAsyncClient sqsClient,
                        @Value("${aws.sqs.order.queue}") String queue,
                        OrderRepository orderRepository,
                        OrderMapper mapper,
                        ObjectMapper objectMapper) {
        this.messagingPort = messagingPort;
        this.sqsClient = sqsClient;
        this.queue = queue;
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Order> publishOrderCommand(Order order) {
        return Mono.just(serializePayload().unchecked().apply(order))
            .zipWith(getQueueUrl().apply(queue))
            .map(t -> buildMessageRequest().unchecked().apply(t))
            .doOnError(throwable -> LOGGER.error("Failed to prepare message due to error.", throwable))
            .flatMap(message -> Mono.fromFuture(sqsClient.sendMessage(message)))
            .doOnError(throwable -> LOGGER.error("Failed to send message due to error.", throwable))
            .doOnSuccess(response ->
                LOGGER.debug("Message published to queue. Message ID: {} Body: {}", response.messageId(),
                    response.md5OfMessageBody()))
            .map(__ -> order);
    }

    @Override
    public Flux<Message> readOrder(Function1<Order, Mono<Order>> handle) {
        return messagingPort.read(queue, handle, readEvent());
    }

    @Override
    public Flux<Message> readOrderCancel(Function1<Order, Mono<Order>> handle) {
        return messagingPort.read(queue, handle, readEvent());
    }

    private CheckedFunction1<Message, Order> readEvent() {
        return message -> objectMapper.readValue(message.body(), Order.class);
    }

    @Override
    @Transactional
    public Mono<Order> createOrder(Order order) {
        return orderRepository.save(mapper.entityFromDomain(order))
            .map(mapper::domainFromEntity);
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

    private Function1<String, Mono<GetQueueUrlResponse>> getQueueUrl() {
        return queueName -> Mono.fromFuture(sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build()))
            .doOnError(throwable -> LOGGER.error("Failed to get queueUrl", throwable));
    }

    private CheckedFunction1<Tuple2<String, GetQueueUrlResponse>, SendMessageRequest> buildMessageRequest() {
        return t -> SendMessageRequest.builder()
            .messageBody(t.getT1())
            .queueUrl(t.getT2().queueUrl())
            .build();
    }
}
