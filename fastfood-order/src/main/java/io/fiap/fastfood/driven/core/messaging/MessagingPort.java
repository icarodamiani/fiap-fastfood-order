package io.fiap.fastfood.driven.core.messaging;

import io.vavr.CheckedFunction1;
import io.vavr.Function1;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

public interface MessagingPort {

    Flux<Message> read(String queue);

    Mono<Message> ack(String queue, Message message);

    <T> Mono<T> send(String queue, T payload, CheckedFunction1<T, String> serialize);
}
