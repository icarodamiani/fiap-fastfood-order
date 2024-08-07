package io.fiap.fastfood.driven.core.domain.order.mapper;

import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    @Mapping(source = "customer.vat", target = "customerId")
    OrderEntity entityFromDomain(Order order);

    Order domainFromEntity(OrderEntity orderEntity);


}
