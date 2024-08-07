package io.fiap.fastfood.driven.core.domain.customer.port.outbound;

import io.fiap.fastfood.driven.core.domain.model.Customer;
import reactor.core.publisher.Mono;

public interface CustomerPort {

    Mono<Customer> publishCustomerCommand(Customer customer);

}
