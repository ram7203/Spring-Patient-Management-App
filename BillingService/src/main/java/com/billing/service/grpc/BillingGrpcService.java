package com.billing.service.grpc;

import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {

    @Override
    public void createBillingAccount(billing.BillingRequest billingRequest,
                                     StreamObserver<billing.BillingResponse> responseObserver){

        log.info("createBillingAccount request received {}", billingRequest.toString());

        // Business Logic

        // Dummy object
        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId("123")
                .setStatus("ACTIVE")
                .build();

        //Sends response back to the client
        //Separate line is used so that multiple responses can be returned before response is completed
        responseObserver.onNext(response);

        //Indicate response is completed
        responseObserver.onCompleted();

    }
}
