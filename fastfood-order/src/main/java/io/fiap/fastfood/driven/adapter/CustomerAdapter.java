package io.fiap.fastfood.driven.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fiap.fastfood.driven.core.domain.customer.port.outbound.CustomerPort;
import io.fiap.fastfood.driven.core.domain.model.Customer;
import io.fiap.fastfood.driven.core.messaging.MessagingPort;
import io.vavr.CheckedFunction1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomerAdapter implements CustomerPort {

    private final MessagingPort messagingPort;
    private final ObjectMapper objectMapper;
    private final String queue;

    public CustomerAdapter(MessagingPort messagingPort, ObjectMapper objectMapper,
                           @Value("${aws.sqs.customer.queue}") String queue) {
        this.messagingPort = messagingPort;
        this.objectMapper = objectMapper;
        this.queue = queue;
    }

    public Mono<Customer> publishCustomerCommand(Customer customer) {
        return messagingPort.send(queue, customer, serializePayload());
    }

    private <T> CheckedFunction1<T, String> serializePayload() {
        return objectMapper::writeValueAsString;
    }
}
