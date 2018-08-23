package org.ostelco.prime.analytics

import io.grpc.stub.StreamObserver
import org.ostelco.analytics.grpc.api.OcsgwAnalyticsReport
import org.ostelco.analytics.grpc.api.OcsgwAnalyticsReply
import org.ostelco.analytics.grpc.api.OcsgwAnalyticsServiceGrpc
import org.ostelco.prime.analytics.metrics.OcsgwMetrics
import org.ostelco.prime.logger
import java.util.*


class AnalyticsGrpcService(private val metrics : OcsgwMetrics) : OcsgwAnalyticsServiceGrpc.OcsgwAnalyticsServiceImplBase() {

    private val logger by logger()

    //  rpc OcsgwAnalyticsEvent (stream OcsgwAnalyticsReport) returns (OcsgwAnalyticsReply) {}

    override fun ocsgwAnalyticsEvent(ocsgwAnalyticsReply: StreamObserver<OcsgwAnalyticsReply>): StreamObserver<OcsgwAnalyticsReport> {

        val streamId = newUniqueStreamId()

        return StreamObserverForStreamWithId(streamId)
    }

    /**
     * Return an unique ID based on Java's UUID generator that uniquely
     * identifies a stream of values.
     * @return A new unique identifier.
     */
    private fun newUniqueStreamId(): String {
        return UUID.randomUUID().toString()
    }

    private inner class StreamObserverForStreamWithId internal constructor(private val streamId: String) : StreamObserver<OcsgwAnalyticsReport> {

        /**
         * This method gets called every time a Credit-Control-Request is received
         * from the OCS.
         * @param request
         */
        override fun onNext(request: OcsgwAnalyticsReport) {
            //
        }

        override fun onError(t: Throwable) {
            // TODO vihang: handle onError for stream observers
        }

        override fun onCompleted() {
            logger.info("AnalyticsGrpcService with streamId: {} completed", streamId)

        }
    }
}
