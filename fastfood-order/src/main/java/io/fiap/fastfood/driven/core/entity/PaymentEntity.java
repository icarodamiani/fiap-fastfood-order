package io.fiap.fastfood.driven.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("pagamento")
public record PaymentEntity(
    @Id
    String id,
    @Field("meio_pagamento")
    String method,
    @Field("valor")
    BigDecimal total,
    @Field("data_hora")
    LocalDateTime dateTime,
    @Field("id_pedido")
    String orderId) {

    public static final class PaymentEntityBuilder {
        private String id;
        private String method;
        private BigDecimal total;
        private LocalDateTime dateTime;
        private String orderId;

        private PaymentEntityBuilder() {
        }

        public static PaymentEntityBuilder builder() {
            return new PaymentEntityBuilder();
        }

        public PaymentEntityBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public PaymentEntityBuilder withMethod(String method) {
            this.method = method;
            return this;
        }

        public PaymentEntityBuilder withTotal(BigDecimal amount) {
            this.total = amount;
            return this;
        }

        public PaymentEntityBuilder withDateTime(LocalDateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public PaymentEntityBuilder withOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentEntity build() {
            return new PaymentEntity(id, method, total, dateTime, orderId);
        }
    }

}
