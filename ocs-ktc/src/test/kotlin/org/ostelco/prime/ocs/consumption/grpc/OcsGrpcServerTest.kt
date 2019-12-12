package org.ostelco.prime.ocs.consumption.grpc

import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.CreditControlRequestType.UPDATE_REQUEST
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.ocs.core.OnlineCharging
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis
import kotlin.test.AfterTest

@ExperimentalUnsignedTypes
class OcsGrpcServerTest {

    private lateinit var server: OcsGrpcServer

    @Ignore
    @Test
    fun `load test OCS using gRPC`() = runBlocking {

        // Add delay to DB call and skip analytics and low balance notification
        OnlineCharging.loadUnitTest = true

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

        val durationInMillis = measureTimeMillis {

            // Send the same request COUNT times
            repeat(COUNT) {
                requestStream.onNext(request)
            }

            // Wait for all the responses to be returned
            println("Waiting for all responses to be returned")
            cdl.await()
        }

        requestStream.onCompleted()

        // Print load test results
        println("Time duration: %,d milli sec".format(durationInMillis))
        val rate = COUNT * 1000.0 / durationInMillis
        println("Rate: %,.2f req/sec".format(rate))

        server.forceStop()
    }

    @AfterTest
    fun cleanup() {
        if (::server.isInitialized) {
            server.forceStop()
        }
    }

    companion object {
        private const val COUNT = 100_000
        private const val MSISDN = "4790300147"
    }
}