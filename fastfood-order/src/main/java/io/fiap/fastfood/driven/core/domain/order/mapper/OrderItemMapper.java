package io.fiap.fastfood.driven.core.domain.order.mapper;

import io.fiap.fastfood.driven.core.domain.model.OrderItem;
import io.fiap.fastfood.driven.core.entity.OrderItemEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    OrderItemEntity entityFromDomain(OrderItem orderItem);

    OrderItem domainFromEntity(OrderItemEntity orderItemEntity);
}
