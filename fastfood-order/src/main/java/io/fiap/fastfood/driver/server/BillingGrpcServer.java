package io.fiap.fastfood.driver.server;


import com.google.protobuf.Empty;
import io.fiap.fastfood.BillingServiceGrpc;
import io.fiap.fastfood.CloseBillingDayRequest;
import io.fiap.fastfood.OpenBillingDayRequest;
import io.fiap.fastfood.driven.core.service.BillingService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class BillingGrpcServer extends BillingServiceGrpc.BillingServiceImplBase {

    private final BillingService service;

    private final GrpcStatusConverter statusConverter;

    @Autowired
    public BillingGrpcServer(BillingService service, GrpcStatusConverter statusConverter) {
        this.service = service;
        this.statusConverter = statusConverter;
    }

    @Override
    public void open(OpenBillingDayRequest request, StreamObserver<Empty> responseObserver) {
        service.open()
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .doOnSuccess(response -> {
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            })
            .subscribe();
    }
    @Override
    public void close(CloseBillingDayRequest request, StreamObserver<Empty> responseObserver) {
        service.close()
            .doOnError(throwable -> responseObserver.onError(statusConverter.toGrpcException(throwable)))
            .doOnSuccess(response -> {
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            })
            .subscribe();
    }
}