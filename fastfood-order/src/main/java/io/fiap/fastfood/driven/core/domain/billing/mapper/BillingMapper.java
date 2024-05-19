package io.fiap.fastfood.driven.core.domain.billing.mapper;

import io.fiap.fastfood.driven.core.domain.model.Billing;
import io.fiap.fastfood.driven.core.entity.BillingEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BillingMapper {
    BillingEntity entityFromDomain(Billing billing);

    Billing domainFromEntity(BillingEntity billingEntity);
}
