package com.telenordigital.prime.ocs;

import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.disruptor.PrimeEventProducer;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class OcsService  {

    private final  ConcurrentMap<String, StreamObserver<FetchDataBucketInfo>>
            fetchDataBucketClientMap;

    private final ConcurrentMap<String, StreamObserver<ReturnUnusedDataResponse>>
            returnUnusedDataClientMap;

    /**
     * A holder for
     * {@link io.grpc.stub.StreamObserver < com.telenordigital.prime.ocs.ActivateResponse>}
     * instances that are somehow used
     */
    private final ActivateResponseHolder activateResponseHolder;

    private final PrimeEventProducer producer;

    private final EventHandler<PrimeEvent> eventHandler;

    private final OcsServiceGrpc.OcsServiceImplBase ocsServerImplBaseImpl;

    public OcsService(final PrimeEventProducer producer) {
        this.producer = checkNotNull(producer);
        this.fetchDataBucketClientMap = new ConcurrentHashMap<>();
        this.returnUnusedDataClientMap = new ConcurrentHashMap<>();
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
        producer.returnUnusedDataBucketEvent(
                msisdn,
                bucketBytes,
                null);
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

    protected StreamObserver<FetchDataBucketInfo> getDataBucketClientForStream(
            final String streamId) {
         return fetchDataBucketClientMap.get(streamId);
    }

    protected StreamObserver<ReturnUnusedDataResponse> getUnusedDataClientForStream(
            final String streamId) {
         return returnUnusedDataClientMap.get(streamId);
    }

    protected void activateOnNextResponse(final ActivateResponse response) {
        this.activateResponseHolder.onNextResponse(response);
    }

    protected void updateActivateResponse(
            final StreamObserver<ActivateResponse> activateResponse) {
        this.activateResponseHolder.setActivateResponse(activateResponse);
    }

    protected void removeUnusedDataClient(String streamId) {
        this.returnUnusedDataClientMap.remove(streamId);
    }

    protected void returnUnusedDataBucketEvent(
            final String msisdn,
            final long bytes,
            final String streamId) {
        this.producer.returnUnusedDataBucketEvent(
                msisdn, bytes, streamId);
    }

    protected void registerUnusedDataClient(
            final String streamId,
            final StreamObserver<ReturnUnusedDataResponse>
                    returnUnusedDataResponse) {
        this.returnUnusedDataClientMap.put(streamId, returnUnusedDataResponse);
    }

    protected void deleteDataBucketClient(final String streamId) {
        this.fetchDataBucketClientMap.remove(streamId);
    }

    protected  void fetchDataBucketEvent(
            final FetchDataBucketInfo request,
            final String streamId) {
        producer.injectFetchDataBucketRequestIntoRingbuffer(request, streamId);
    }

    protected void putDataBucketClient(
            final String streamId,
            final StreamObserver<FetchDataBucketInfo> fetchDataBucketResponse) {
        fetchDataBucketClientMap.put(streamId, fetchDataBucketResponse);
    }

    public void replyWithDataBucketInfo(String streamId, FetchDataBucketInfo info) {
        final StreamObserver<FetchDataBucketInfo> fetchDataBucketResponse
                = getDataBucketClientForStream(streamId);

        if (fetchDataBucketResponse != null) {
            fetchDataBucketResponse.onNext(info);
        }
    }

    public void replyWithReturnDataInfo(
            final String ocsgwStreamId,
            final ReturnUnusedDataResponse returnDataInfo) {
        final StreamObserver<ReturnUnusedDataResponse> returnUnusedDataResponse
                =  getUnusedDataClientForStream(ocsgwStreamId);
        if (returnUnusedDataResponse != null) {
            returnUnusedDataResponse.onNext(returnDataInfo);
        }
    }
}
