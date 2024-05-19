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
import io.fiap.fastfood.driven.repository.OrderRepository;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OrderAdapter implements OrderPort {
    private final OrderRepository orderRepository;
    private final OrderMapper mapper;
    private final ObjectMapper objectMapper;

    public OrderAdapter(OrderRepository orderRepository,
                        OrderMapper mapper,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
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
}
