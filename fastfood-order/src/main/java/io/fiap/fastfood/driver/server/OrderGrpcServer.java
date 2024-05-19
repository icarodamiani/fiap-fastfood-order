package io.fiap.fastfood.driver.server;


import com.google.protobuf.Timestamp;
import com.google.type.Decimal;
import io.fiap.fastfood.FindAllOrderRequest;
import io.fiap.fastfood.FindOrderByIdRequest;
import io.fiap.fastfood.OrderItemResponse;
import io.fiap.fastfood.OrderResponse;
import io.fiap.fastfood.OrderServiceGrpc;
import io.fiap.fastfood.PaymentResponse;
import io.fiap.fastfood.SaveOrderItemRequest;
import io.fiap.fastfood.SaveOrderRequest;
import io.fiap.fastfood.SavePaymentRequest;
import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.domain.model.OrderItem;
import io.fiap.fastfood.driven.core.domain.model.Payment;
import io.fiap.fastfood.driven.core.service.OrderService;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@GrpcService
public class OrderGrpcServer extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderService service;

    private final GrpcStatusConverter statusConverter;

    @Autowired
    public OrderGrpcServer(OrderService service, GrpcStatusConverter statusConverter) {
        this.service = service;
        this.statusConverter = statusConverter;
    }

    @Override
    public void saveOrder(SaveOrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        service.create(Order.OrderBuilder.builder()
                .withCustomerId(request.getCustomerId())
                .withCreatedAt(toLocalDate(request.getCreatedAt()))
                .withNumber(request.getNumber())
                .withPayment(toPayment(request.getPayment()))
                .withItems(toOrderItems(request.getItemsList()))
                .build())
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(order ->
                OrderResponse.newBuilder()
                    .setId(order.id())
                    .build()
            )
            .map(response -> {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return response;
            })
            .subscribe();
    }

    private List<OrderItem> toOrderItems(List<SaveOrderItemRequest> items) {
        return items.stream()
            .map(item -> {
                var builder = OrderItem.OrderItemBuilder.builder()
                    .withProductId(item.getProductId())
                    .withQuote(item.getQuote());

                if (item.hasAmount()) {
                    builder.withAmount(new BigDecimal(item.getAmount().getValue()));
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static Payment toPayment(SavePaymentRequest paymentRequest) {
        var builder = Payment.PaymentBuilder.builder();
        if (paymentRequest.isInitialized()) {
            if (paymentRequest.hasTotal()) {
                builder.withTotal(new BigDecimal(paymentRequest.getTotal().getValue()));
            }
            if (paymentRequest.hasDateTime()) {
                builder.withDateTime(toLocalDate(paymentRequest.getDateTime()));
            }
            builder.withMethod(paymentRequest.getMethod());
        }
        return builder.build();
    }

    private static LocalDateTime toLocalDate(Timestamp ts) {
        return Instant
            .ofEpochSecond(ts.getSeconds(), ts.getNanos())
            .atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC))
            .toLocalDateTime();
    }

    @Override
    public void findAllOrders(FindAllOrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        service.findAll(Pageable.unpaged())
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(order -> OrderResponse.newBuilder()
                .setId(order.id())
                .setCreatedAt(toTimestamp(order.createdAt()))
                .setCustomerId(order.customerId())
                .setNumber(order.number())
                .addAllItems(order.items().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList()))
                .build()
            )
            .map(response -> {
                responseObserver.onNext(response);
                return response;
            })
            .doOnComplete(responseObserver::onCompleted)
            .subscribe();
    }

    @Override
    public void findOrderById(FindOrderByIdRequest request, StreamObserver<OrderResponse> responseObserver) {
        service.findById(request.getId())
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(order -> OrderResponse.newBuilder()
                .setId(order.id())
                .setCreatedAt(toTimestamp(order.createdAt()))
                .setCustomerId(order.customerId())
                .setNumber(order.number())
                .addAllItems(order.items().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList()))
                .build()
            )
            .map(response -> {
                responseObserver.onNext(response);
                return response;
            })
            .doOnSuccess(__ -> responseObserver.onCompleted())
            .subscribe();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.newBuilder()
            .setOrderId(payment.orderId())
            .setMethod(payment.method())
            .setDateTime(toTimestamp(payment.dateTime()))
            .setTotal(toDecimal(payment.total()))
            .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.newBuilder()
            .setQuote(item.quote())
            .setProductId(item.productId())
            .setAmount(toDecimal(item.amount()))
            .build();
    }

    protected Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime != null) {
            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

            return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
        }
        return null;
    }

    private static Decimal toDecimal(BigDecimal value) {
        return Decimal.newBuilder().setValue(value.toString()).build();
    }
}