package org.ostelco.prime.ocs.consumption

import arrow.core.right
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.CreditControlRequestType.UPDATE_REQUEST
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.ocs.core.OnlineCharging
import org.ostelco.prime.ocs.mockGraphStore
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.streams.toList
import kotlin.test.AfterTest
import kotlin.test.fail

class OcsGrpcServerTest {

    private lateinit var server: OcsGrpcServer

    @Ignore
    @Test
    fun `load test OCS using gRPC`() {

        // Add delay to DB call and skip analytics and low balance notification
        OnlineCharging.loadUnitTest = true

        // call to graphStore always return 100 as reserved Bucket bytes and 200 as balance Bundle bytes
        Mockito.`when`(mockGraphStore.consume(MSISDN, 80, 100))
                .thenReturn(Pair(100L, 200L).right())

        server = OcsGrpcServer(8082, OcsGrpcService(OnlineCharging))

        server.start()

        // Setup gRPC client
        val channel = ManagedChannelBuilder
                .forTarget("localhost:8082")
                .usePlaintext()
                .build()

        val ocsService = OcsServiceGrpc.newStub(channel)

        // count down latch to wait for all responses to return
        val cdl = CountDownLatch(COUNT)

        // response handle which will count down on receiving response
        val requestStream = ocsService.creditControlRequest(object : StreamObserver<CreditControlAnswerInfo> {

            override fun onNext(value: CreditControlAnswerInfo?) {
                // count down on receiving response
                cdl.countDown()
            }

            override fun onError(t: Throwable?) {
                fail(t?.message)
            }

            override fun onCompleted() {

            }
        })

        // Sample request which will be sent repeatedly
        val request = CreditControlRequestInfo.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setType(UPDATE_REQUEST)
                .setMsisdn(MSISDN)
                .addMscc(0, MultipleServiceCreditControl.newBuilder()
                        .setRequested(ServiceUnit.newBuilder().setTotalOctets(100))
                        .setUsed(ServiceUnit.newBuilder().setTotalOctets(80)))
                .build()

        // Start timestamp in millisecond
        val start = Instant.now()

        // Send the same request COUNT times
        (1..COUNT)
                .toList()
                .parallelStream()
                .map { _ -> requestStream.onNext(request) }
                .toList()

        // Wait for all the responses to be returned
        println("Waiting for all responses to be returned")
        cdl.await()

        // Stop timestamp in millisecond
        val stop = Instant.now()

        requestStream.onCompleted()

        // Print load test results
        val diff = stop.toEpochMilli() - start.toEpochMilli()
        println("Time diff: %,d milli sec".format(diff))
        val rate = COUNT * 1000.0 / diff
        println("Rate: %,.2f req/sec".format(rate))

        server.stop()
    }

    @AfterTest
    fun cleanup() {
        if (::server.isInitialized) {
            server.stop()
        }
    }

    companion object {
        private const val COUNT = 100_000
        private const val MSISDN = "4790300147"
    }
}