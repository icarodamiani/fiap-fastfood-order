package io.fiap.fastfood.driven.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.fiap.fastfood.driven.client.PaymentClient;
import io.fiap.fastfood.driven.core.domain.model.Payment;
import io.fiap.fastfood.driven.core.domain.payment.mapper.PaymentMapper;
import io.fiap.fastfood.driven.core.domain.payment.port.outbound.PaymentPort;
import io.fiap.fastfood.driven.core.entity.PaymentEntity;
import io.fiap.fastfood.driven.core.exception.BadRequestException;
import io.fiap.fastfood.driven.core.exception.DuplicatedKeyException;
import io.fiap.fastfood.driven.repository.PaymentRepository;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import io.vavr.Function1;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class PaymentAdapter implements PaymentPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentAdapter.class);


    private final PaymentClient paymentClient;

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsClient;

    private final String paymentQueue;

    public PaymentAdapter(PaymentRepository repository,
                          PaymentMapper mapper,
                          ObjectMapper objectMapper,
                          PaymentClient paymentClient,
                          SqsAsyncClient sqsClient,
                          @Value("${payment.sqs.queue}") String paymentQueue) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.paymentClient = paymentClient;
        this.sqsClient = sqsClient;
        this.paymentQueue = paymentQueue;
    }


    @Override
    public Mono<Payment> createPayment(Payment payment) {
        return repository.findByOrderId(payment.orderId())
            .next()
            .flatMap(c -> Mono.defer(() -> Mono.<PaymentEntity>error(DuplicatedKeyException::new)))
            .switchIfEmpty(Mono.defer(() -> repository.save(mapper.entityFromDomain(payment))
                .flatMap(entity -> paymentClient.createPayment(mapper.domainFromEntity(entity))
                    .map(response -> entity))))
            .map(mapper::domainFromEntity);
    }

    @Override
    public Mono<Payment> updatePayment(String id, String operations) {
        return repository.findById(id)
            .map(payment -> applyPatch().unchecked().apply(payment, operations))
            .flatMap(repository::save)
            .map(mapper::domainFromEntity)
            .doOnSuccess(this::sendPaymentUpdatedMessage)
            .onErrorMap(JsonPatchException.class::isInstance, BadRequestException::new);
    }

    private CheckedFunction2<PaymentEntity, String, PaymentEntity> applyPatch() {
        return (payment, operations) -> {
            var patch = readOperations()
                .unchecked()
                .apply(operations);

            var patched = patch.apply(objectMapper.convertValue(payment, JsonNode.class));

            return objectMapper.treeToValue(patched, PaymentEntity.class);
        };
    }

    private CheckedFunction1<String, JsonPatch> readOperations() {
        return operations -> {
            final InputStream in = new ByteArrayInputStream(operations.getBytes());
            return objectMapper.readValue(in, JsonPatch.class);
        };
    }

    public Mono<Void> sendPaymentUpdatedMessage(Payment payload) {
        return Mono.just(serializePayload().unchecked().apply(payload))
            .zipWith(getQueueUrl().apply(paymentQueue))
            .map(t -> buildMessageRequest().unchecked().apply(t))
            .doOnError(throwable -> LOGGER.error("Failed to prepare message due to error. {}", throwable.getMessage()))
            .flatMap(message -> Mono.fromFuture(sqsClient.sendMessage(message)))
            .doOnError(throwable -> LOGGER.error("Failed to send message due to error. {}", throwable.getMessage()))
            .doOnSuccess(response ->
                LOGGER.debug("Message published to queue. Message ID: {} Body:  {}", response.messageId(),
                    response.md5OfMessageBody()))
            .then();
    }

    private <T> CheckedFunction1<T, String> serializePayload() {
        return objectMapper::writeValueAsString;
    }

    private Function1<String, Mono<GetQueueUrlResponse>> getQueueUrl() {
        return queue -> Mono.fromFuture(sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queue)
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
