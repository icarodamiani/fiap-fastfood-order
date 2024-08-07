package io.fiap.fastfood.driven.core.domain.model;

public record Customer(
    String id,
    String name,
    String vat,
    String email,
    String phone) {

    public static final class CustomerBuilder {
        private String id;
        private String name;
        private String vat;
        private String email;
        private String phone;

        private CustomerBuilder() {
        }

        public static CustomerBuilder builder() {
            return new CustomerBuilder();
        }

        public CustomerBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public CustomerBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public CustomerBuilder withVat(String vat) {
            this.vat = vat;
            return this;
        }

        public CustomerBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public CustomerBuilder withPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Customer build() {
            return new Customer(id, name, vat, email, phone);
        }
    }
}

