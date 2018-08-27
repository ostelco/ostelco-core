package org.ostelco.prime.analytics

import io.grpc.stub.StreamObserver
import org.ostelco.prime.analytics.metrics.OcsgwMetrics
import org.ostelco.prime.logger
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReply
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport
import org.ostelco.prime.metrics.api.OcsgwAnalyticsServiceGrpc
import java.util.*


/**
 * Serves incoming GRPC analytcs requests.
 *
 * It's implemented as a subclass of [OcsServiceGrpc.OcsServiceImplBase] overriding
 * methods that together implements the protocol described in the  analytics protobuf
 * file: ocs_analytics.proto
 *`
 * service OcsgwAnalyticsService {
 *    rpc OcsgwAnalyticsEvent (stream OcsgwAnalyticsReport) returns (OcsgwAnalyticsReply) {}
 * }
 */

class AnalyticsGrpcService(private val metrics : OcsgwMetrics) : OcsgwAnalyticsServiceGrpc.OcsgwAnalyticsServiceImplBase() {

    private val logger by logger()

    /**
     * Handles the OcsgwAnalyticsEvent message.
     */
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
         * This method gets called every time a new active session count is sent
         * from the OCS GW.
         * @param request provides current active session as a counter with a timestamp
         */
        override fun onNext(request: OcsgwAnalyticsReport) {
            metrics.setActiveSessions(request.activeSessions.toLong())
        }

        override fun onError(t: Throwable) {
            // TODO vihang: handle onError for stream observers
        }

        override fun onCompleted() {
            logger.info("AnalyticsGrpcService with streamId: {} completed", streamId)
        }
    }
}
