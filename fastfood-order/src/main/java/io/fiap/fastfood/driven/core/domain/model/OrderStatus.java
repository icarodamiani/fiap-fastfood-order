package io.fiap.fastfood.driven.core.domain.model;

import java.util.Optional;

public record OrderStatus(
    Long id,
    String name,
    String description,
    Boolean customerFriendly) {

    Optional<Long> getId() {
        return Optional.ofNullable(id());
    }

    Optional<String> getName() {
        return Optional.ofNullable(name());
    }

    Optional<String> getDescription() {
        return Optional.ofNullable(description());
    }

    Optional<Boolean> getCustomerFriendly() {
        return Optional.ofNullable(customerFriendly());
    }

    public static final class OrderStatusBuilder {
        private Long id;
        private String name;
        private String description;
        private Boolean customerFriendly;

        private OrderStatusBuilder() {
        }

        public static OrderStatusBuilder builder() {
            return new OrderStatusBuilder();
        }

        public OrderStatusBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public OrderStatusBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public OrderStatusBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public OrderStatusBuilder withCustomerFriendly(Boolean customerFriendly) {
            this.customerFriendly = customerFriendly;
            return this;
        }

        public OrderStatus build() {
            return new OrderStatus(id, name, description, customerFriendly);
        }
    }
}
