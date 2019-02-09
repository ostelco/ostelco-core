package org.ostelco.at.loadtest

import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.junit.Test
import org.ostelco.at.common.createProfile
import org.ostelco.at.common.createSubscription
import org.ostelco.at.common.ocsSocket
import org.ostelco.at.jersey.get
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.CreditControlRequestType.UPDATE_REQUEST
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.client.model.BundleList
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.streams.toList
import kotlin.test.assertEquals
import kotlin.test.fail

class OcsLoadTest {

    /**
     * Start docker compose locally before running this test.
     *
     * 'doco -f docker-compose.ocs.yaml up --build'
     */
    @Test
    fun `load test OCS using gRPC`() {

        val users = (1..USER_COUNT)
                .toList()
                .parallelStream()
                .map { i ->
                    val email = "ocs-load-test-$i@test.com"

                    // Create Customer
                    createProfile(name = "OCS Load Test", email = email)

                    // Assign MSISDN to customer
                    val msisdn = createSubscription(email = email)

                    User(email = email, msisdn = msisdn)
                }
                .toList()

        // Setup gRPC client
        val channel = ManagedChannelBuilder
                .forTarget(ocsSocket)
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

        // Start timestamp in millisecond
        val start = Instant.now()

        // Send the same request COUNT times
        (0 until COUNT)
                .toList()
                .parallelStream()
                .map { i ->

                    // Sample request which will be sent repeatedly
                    val request = CreditControlRequestInfo.newBuilder()
                            .setRequestId(UUID.randomUUID().toString())
                            .setType(UPDATE_REQUEST)
                            .setMsisdn(users[i % USER_COUNT].msisdn)
                            .addMscc(0, MultipleServiceCreditControl.newBuilder()
                                    .setRequested(ServiceUnit.newBuilder().setTotalOctets(REQUESTED_BYTES))
                                    .setUsed(ServiceUnit.newBuilder().setTotalOctets(USED_BYTES)))
                            .build()

                    requestStream.onNext(request)
                }
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

        //for (i in 0 until USER_COUNT) {
        // check balance
        val bundles: BundleList = get {
            path = "/bundles"
            this.email = users[0].email
        }

        val balance = bundles[0].balance

        // initial balance = 100 MB
        // consumed = COUNT * USED_BYTES
        // reserved = REQUESTED_BYTES
        assertEquals(
                expected = 100_000_000 - COUNT * USED_BYTES / USER_COUNT - REQUESTED_BYTES,
                actual = balance,
                message = "Balance does not match after load test consumption")
        //}
    }

    companion object {
        private const val USER_COUNT = 100
        private const val COUNT = 10_000
        private const val USED_BYTES = 10L
        private const val REQUESTED_BYTES = 100L
    }
}

data class User(val email: String, val msisdn: String)