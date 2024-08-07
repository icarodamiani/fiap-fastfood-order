package io.fiap.fastfood.driven.core.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record Order(
    String id,
    Customer customer,
    List<OrderItem> items,
    LocalDateTime createdAt,
    String number,
    Payment payment) {

    public Optional<String> getId() {
        return Optional.ofNullable(id());
    }

    public Optional<Customer> getCustomer() {
        return Optional.ofNullable(customer());
    }

    public Optional<String> getNumber() {
        return Optional.ofNullable(number());
    }

    public Optional<Payment> getPayment() {
        return Optional.ofNullable(payment());
    }

    public static final class OrderBuilder {
        private String id;
        private Customer customer;
        private List<OrderItem> items;
        private LocalDateTime createdAt;
        private String number;
        private Payment payment;

        private OrderBuilder() {
        }

        public static OrderBuilder builder() {
            return new OrderBuilder();
        }

        public static OrderBuilder from(Order order) {
            return OrderBuilder.builder()
                .withId(order.id)
                .withNumber(order.number)
                .withCreatedAt(order.createdAt)
                .withItems(order.items)
                .withCustomer(order.customer);
        }

        public OrderBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public OrderBuilder withCustomer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public OrderBuilder withItems(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        public OrderBuilder withCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderBuilder withNumber(String number) {
            this.number = number;
            return this;
        }

        public OrderBuilder withPayment(Payment payment) {
            this.payment = payment;
            return this;
        }

        public Order build() {
            return new Order(id, customer, items, createdAt, number, payment);
        }
    }
}
