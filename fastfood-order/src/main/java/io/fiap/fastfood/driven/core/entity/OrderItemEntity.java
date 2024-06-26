package io.fiap.fastfood.driven.core.entity;

import org.springframework.data.mongodb.core.mapping.Field;

public record OrderItemEntity(
    @Field
    String productId,

    @Field
    Integer amount,

    @Field
    String quote) {

    public static final class OrderItemEntityBuilder {
        private String productId;
        private Integer amount;
        private String quote;

        private OrderItemEntityBuilder() {
        }

        public static OrderItemEntityBuilder builder() {
            return new OrderItemEntityBuilder();
        }


        public OrderItemEntityBuilder withProductId(String productId) {
            this.productId = productId;
            return this;
        }

        public OrderItemEntityBuilder withAmount(Integer amount) {
            this.amount = amount;
            return this;
        }

        public OrderItemEntityBuilder withQuote(String quote) {
            this.quote = quote;
            return this;
        }

        public OrderItemEntity build() {
            return new OrderItemEntity(productId, amount, quote);
        }
    }

}
