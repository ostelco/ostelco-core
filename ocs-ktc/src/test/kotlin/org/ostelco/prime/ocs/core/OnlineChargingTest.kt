package org.ostelco.prime.ocs.core

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.ServiceUnit
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis
import kotlin.test.fail

@ExperimentalUnsignedTypes
class OnlineChargingTest {

    @Ignore
    @Test
    fun `load test OnlineCharging directly`() = runBlocking {

        // Add delay to DB call and skip analytics and low balance notification
        OnlineCharging.loadUnitTest = true

        // count down latch to wait for all responses to return
        val cdl = CountDownLatch(COUNT)

        // response handle which will count down on receiving response
        val creditControlAnswerInfo: StreamObserver<CreditControlAnswerInfo> = object : StreamObserver<CreditControlAnswerInfo> {

            override fun onNext(value: CreditControlAnswerInfo?) {
                // count down on receiving response
                cdl.countDown()
            }

            override fun onError(t: Throwable?) {
                fail(t?.message)
            }

            override fun onCompleted() {

            }
        }

        // Sample request which will be sent repeatedly
        val request = CreditControlRequestInfo.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setMsisdn(MSISDN)
                .addMscc(0, MultipleServiceCreditControl.newBuilder()
                        .setRequested(ServiceUnit.newBuilder().setTotalOctets(100))
                        .setUsed(ServiceUnit.newBuilder().setTotalOctets(80)))
                .build()

        val durationInMillis = measureTimeMillis {

            // Send the same request COUNT times
            repeat(COUNT) {
                OnlineCharging.creditControlRequestEvent(
                        request = request,
                        returnCreditControlAnswer = creditControlAnswerInfo::onNext)
            }

            // Wait for all the responses to be returned
            println("Waiting for all responses to be returned")

            @Suppress("BlockingMethodInNonBlockingContext")
            cdl.await()
        }

        // Print load test results
        println("Time duration: %,d milli sec".format(durationInMillis))
        val rate = COUNT * 1000.0 / durationInMillis
        println("Rate: %,.2f req/sec".format(rate))
    }

    companion object {
        private const val COUNT = 1_000_000
        private const val MSISDN = "4790300147"
    }
}