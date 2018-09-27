package org.ostelco.prime.analytics

import io.grpc.stub.StreamObserver
import org.ostelco.prime.analytics.PrimeMetric.ACTIVE_SESSIONS
import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry

import org.ostelco.prime.analytics.publishers.ActiveUsersPublisher
import org.ostelco.prime.getLogger
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReply
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport
import org.ostelco.prime.metrics.api.OcsgwAnalyticsServiceGrpc
import java.util.*


/**
 * Serves incoming GRPC analytics requests.
 *
 * It's implemented as a subclass of [OcsServiceGrpc.OcsServiceImplBase] overriding
 * methods that together implements the protocol described in the  analytics protobuf
 * file: ocs_analytics.proto
 *`
 * service OcsgwAnalyticsService {
 *    rpc OcsgwAnalyticsEvent (stream OcsgwAnalyticsReport) returns (OcsgwAnalyticsReply) {}
 * }
 */

class AnalyticsGrpcService : OcsgwAnalyticsServiceGrpc.OcsgwAnalyticsServiceImplBase() {

    private val logger by getLogger()

    /**
     * Handles the OcsgwAnalyticsEvent message.
     */
    override fun ocsgwAnalyticsEvent(ocsgwAnalyticsReply: StreamObserver<OcsgwAnalyticsReply>): StreamObserver<OcsgwAnalyticsReport> {
        val streamId = newUniqueStreamId()
        return StreamObserverForStreamWithId(streamId)
    }

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
            if (!request.keepAlive) {
                CustomMetricsRegistry.updateMetricValue(ACTIVE_SESSIONS, request.activeSessions.toLong())
                ActiveUsersPublisher.publish(request.usersList)
            }
        }

        override fun onError(t: Throwable) {
            // TODO vihang: handle onError for stream observers
        }

        override fun onCompleted() {
            logger.info("AnalyticsGrpcService with streamId: {} completed", streamId)
        }
    }
}
