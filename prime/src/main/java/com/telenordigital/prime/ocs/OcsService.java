package com.telenordigital.prime.ocs;

import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.disruptor.PrimeEventProducer;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

public final class OcsService  {

    private final  ConcurrentMap<String, StreamObserver<CreditControlAnswerInfo>>
            CreditControlClientMap;

    /**
     * A holder for
     * {@link io.grpc.stub.StreamObserver < com.telenordigital.prime.ocs.ActivateResponse>}
     * instances that are somehow used
     */
    private ActivateResponseHolder activateResponseHolder;

    private final PrimeEventProducer producer;

    private final EventHandler<PrimeEvent> eventHandler;

    private final OcsServiceGrpc.OcsServiceImplBase ocsServerImplBaseImpl;

    public OcsService(final PrimeEventProducer producer) {
        this.producer = checkNotNull(producer);
        this.CreditControlClientMap = new ConcurrentHashMap<>();
        this.eventHandler = new EventHandlerImpl(this);
        this.ocsServerImplBaseImpl = new OcsGRPCService(this );
        this.activateResponseHolder = new ActivateResponseHolder();
    }

    public EventHandler<PrimeEvent> asEventHandler() {
        return eventHandler;
    }

    protected void returnUnusedDataBucketEvent(
            final String msisdn,
            final long bucketBytes) {
        producer.releaseReservedDataBucketEvent(
                msisdn,
                bucketBytes);
    }

    /**
     * Return a service that can be used to serve incoming GRPC requests.   The service
     * is typically bound to a service port using the GRPC ServerBuilder mechanism
     * provide by GRPC:
     * <code>
     *     server = ServerBuilder.
     *         forPort(port).
     *         addService(service).
     *         build();
     * </code>
     *
     * @return The service that can receive incoming GPRS messages
     */
    public OcsServiceGrpc.OcsServiceImplBase asOcsServiceImplBase() {
        return this.ocsServerImplBaseImpl;
    }

    protected StreamObserver<CreditControlAnswerInfo> getCreditControlClientForStream(
            final String streamId) {
            // Here we need to Convert it back to an answer.
            CreditControlClientMap.get(streamId);
         return CreditControlClientMap.get(streamId);
    }

    protected void activateOnNextResponse(final ActivateResponse response) {
        this.activateResponseHolder.onNextResponse(response);
    }

    protected void updateActivateResponse(
            final StreamObserver<ActivateResponse> activateResponse) {
        this.activateResponseHolder.setActivateResponse(activateResponse);
    }

    protected void deleteCreditControlClient(final String streamId) {
        this.CreditControlClientMap.remove(streamId);
    }

    protected  void creditControlRequestEvent(
            final CreditControlRequestInfo request,
            final String streamId) {
        producer.injectCreditControlRequestIntoRingbuffer(request, streamId);
    }

    protected void putCreditControlClient(
            final String streamId,
            final StreamObserver<CreditControlAnswerInfo> creditControlAnswer) {
        CreditControlClientMap.put(streamId, creditControlAnswer);
    }

    public void sendCreditControlAnswer(String streamId, CreditControlAnswerInfo creditControlAnswerInfo) {
        final StreamObserver<CreditControlAnswerInfo> fetchDataBucketResponse
                = getCreditControlClientForStream(streamId);

        if (fetchDataBucketResponse != null) {
            fetchDataBucketResponse.onNext(creditControlAnswerInfo);
        }
    }
}
