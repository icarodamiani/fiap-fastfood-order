package io.fiap.fastfood.driven.core.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document("ponto_venda")
public record BillingEntity(
    @Id
    String id,
    @Field("data_hora_abertura")
    LocalDateTime openAt,
    @Field("data_hora_fechamento")
    LocalDateTime closedAt) {

    public static final class BillingEntityBuilder {
        private String id;
        private LocalDateTime openAt;
        private LocalDateTime closedAt;

        private BillingEntityBuilder() {
        }

        public static BillingEntityBuilder builder() {
            return new BillingEntityBuilder();
        }

        public static BillingEntityBuilder from(BillingEntity entity) {
            return new BillingEntityBuilder()
                .withId(entity.id())
                .withOpenAt(entity.openAt)
                .withClosedAt(entity.closedAt);
        }

        public BillingEntityBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public BillingEntityBuilder withOpenAt(LocalDateTime openAt) {
            this.openAt = openAt;
            return this;
        }

        public BillingEntityBuilder withClosedAt(LocalDateTime closedAt) {
            this.closedAt = closedAt;
            return this;
        }

        public BillingEntity build() {
            return new BillingEntity(id, openAt, closedAt);
        }
    }
}
