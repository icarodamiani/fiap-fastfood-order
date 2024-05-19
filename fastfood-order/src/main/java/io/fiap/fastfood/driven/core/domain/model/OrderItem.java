package io.fiap.fastfood.driven.core.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

public record OrderItem(
    String productId,
    BigDecimal amount,
    String quote) {

    Optional<String> getQuote() {
        return Optional.ofNullable(quote());
    }


    public static final class OrderItemBuilder {
        private String productId;
        private BigDecimal amount;
        private String quote;

        private OrderItemBuilder() {}

        public static OrderItemBuilder builder() {return new OrderItemBuilder();}

        public OrderItemBuilder withProductId(String productId) {
            this.productId = productId;
            return this;
        }

        public OrderItemBuilder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public OrderItemBuilder withQuote(String quote) {
            this.quote = quote;
            return this;
        }

        public OrderItem build() {return new OrderItem(productId, amount, quote);}
    }
}
