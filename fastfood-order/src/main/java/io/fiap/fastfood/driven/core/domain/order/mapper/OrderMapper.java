package io.fiap.fastfood.driven.core.domain.order.mapper;

import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.domain.payment.mapper.PaymentMapper;
import io.fiap.fastfood.driven.core.entity.OrderEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class, PaymentMapper.class})
public interface OrderMapper {
    OrderEntity entityFromDomain(Order order);

    Order domainFromEntity(OrderEntity orderEntity);
}
