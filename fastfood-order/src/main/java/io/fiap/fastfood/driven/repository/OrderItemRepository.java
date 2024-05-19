package io.fiap.fastfood.driven.repository;

import io.fiap.fastfood.driven.core.entity.OrderItemEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItemEntity, String> {
}
