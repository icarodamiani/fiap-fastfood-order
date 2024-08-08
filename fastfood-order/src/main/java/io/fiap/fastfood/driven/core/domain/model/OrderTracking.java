package io.fiap.fastfood.driven.core.domain.model;

public record OrderTracking(
        String orderNumber,
        String orderStatus,
        String orderStatusValue
) {

    public static final class OrderTrackingBuilder {
        private String orderNumber;
        private String orderStatus;
        private String orderStatusValue;

        private OrderTrackingBuilder() {
        }

        public static OrderTrackingBuilder builder() {
            return new OrderTrackingBuilder();
        }

        public static OrderTrackingBuilder from(OrderTracking tracking) {
            return OrderTrackingBuilder.builder()
                .withOrderNumber(tracking.orderNumber)
                .withOrderStatus(tracking.orderStatus)
                .withOrderStatusValue(tracking.orderStatusValue);
        }


        public OrderTrackingBuilder withOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }


        public OrderTrackingBuilder withOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
            return this;
        }

        public OrderTrackingBuilder withOrderStatusValue(String orderStatusValue) {
            this.orderStatusValue = orderStatusValue;
            return this;
        }


        public OrderTracking build() {
            return new OrderTracking(orderNumber, orderStatus, orderStatusValue);
        }
    }
}
