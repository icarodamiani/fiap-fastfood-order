package io.fiap.fastfood.driven.core.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("counters")
public record CounterEntity(
    @Field("name")
    String name,
    @Field("sequence")
    Long sequence) {


    public static final class CountersEntityBuilder {
        private String name;
        private Long sequence;

        private CountersEntityBuilder() {
        }

        public static CountersEntityBuilder builder() {
            return new CountersEntityBuilder();
        }

        public static CountersEntityBuilder from(CounterEntity entity) {
            return new CountersEntityBuilder().withName(entity.name).withSequence(entity.sequence);
        }

        public CountersEntityBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public CountersEntityBuilder withSequence(Long sequence) {
            this.sequence = sequence;
            return this;
        }

        public CounterEntity build() {
            return new CounterEntity(name, sequence);
        }
    }
}

