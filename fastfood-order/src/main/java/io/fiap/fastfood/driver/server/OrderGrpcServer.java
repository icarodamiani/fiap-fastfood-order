package io.fiap.fastfood.driver.server;


import com.google.protobuf.Timestamp;
import io.fiap.fastfood.FindAllOrderRequest;
import io.fiap.fastfood.FindOrderByIdRequest;
import io.fiap.fastfood.OrderItemResponse;
import io.fiap.fastfood.OrderResponse;
import io.fiap.fastfood.OrderServiceGrpc;
import io.fiap.fastfood.SaveCustomerRequest;
import io.fiap.fastfood.SaveOrderItemRequest;
import io.fiap.fastfood.SaveOrderRequest;
import io.fiap.fastfood.SavePaymentRequest;
import io.fiap.fastfood.driven.core.domain.model.Customer;
import io.fiap.fastfood.driven.core.domain.model.Order;
import io.fiap.fastfood.driven.core.domain.model.OrderItem;
import io.fiap.fastfood.driven.core.domain.model.Payment;
import io.fiap.fastfood.driven.core.service.OrderService;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

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
                .withCustomer(toCustomer(request.getCustomer()))
                .withPayment(toPayment(request.getPayment()))
                .withItems(toOrderItems(request.getItemsList()))
                .build())
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(order ->
                OrderResponse.newBuilder()
                    .setId(order.id())
                    .setNumber(order.number())
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
            .map(item -> OrderItem.OrderItemBuilder.builder()
                .withProductId(item.getProductId())
                .withQuote(item.getQuote())
                .withAmount(item.getAmount())
                .build())
            .collect(Collectors.toList());
    }

    private static Payment toPayment(SavePaymentRequest paymentRequest) {
        var builder = Payment.PaymentBuilder.builder();
        if (paymentRequest.isInitialized()) {
            if (paymentRequest.hasTotal()) {
                builder.withTotal(new BigDecimal(paymentRequest.getTotal().getValue()));
            }
            builder.withMethod(paymentRequest.getMethod());
        }
        return builder.build();
    }

    private static Customer toCustomer(SaveCustomerRequest customerRequest) {
        var builder = Customer.CustomerBuilder.builder();
        if (customerRequest.isInitialized()) {
            if (customerRequest.hasId()) {
                builder.withId(customerRequest.getId());
            }
            builder.withName(customerRequest.getName());
            builder.withEmail(customerRequest.getEmail());
            if (customerRequest.hasPhone()) {
                builder.withPhone(customerRequest.getPhone());
            }
            builder.withVat(customerRequest.getVat());
        }
        return builder.build();
    }

    @Override
    public void findAllOrders(FindAllOrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        service.findAll(PageRequest.of(request.getPage(), request.getPageSize()))
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .map(order -> OrderResponse.newBuilder()
                .setId(order.id())
                .setCreatedAt(toTimestamp(order.createdAt()))
                .setCustomerId(order.getCustomer().orElse(Customer.CustomerBuilder.builder().build()).id())
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
                .setCustomerId(order.getCustomer().orElse(Customer.CustomerBuilder.builder().build()).id())
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

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.newBuilder()
            .setQuote(item.quote())
            .setProductId(item.productId())
            .setAmount(item.amount())
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
}