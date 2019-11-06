package org.ostelco.at

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.experimental.ParallelComputer
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.runner.JUnitCore
import org.ostelco.at.common.StripePayment
import org.ostelco.at.common.getLogger
import kotlin.test.assertTrue

class TestSuite {

    private val logger by getLogger()

    @Test
    fun `run all tests in parallel`() {
        runBlocking {

            launch {
                checkResult(
                        JUnitCore.runClasses(
                                ParallelComputer(true, true),
                                org.ostelco.at.okhttp.CustomerTest::class.java,
                                // org.ostelco.at.okhttp.SubscriptionsTest::class.java,
                                org.ostelco.at.okhttp.RegionsTest::class.java,
                                org.ostelco.at.okhttp.SingaporeKycTest::class.java,
                                org.ostelco.at.okhttp.GetProductsTest::class.java,
                                org.ostelco.at.okhttp.BundlesAndPurchasesTest::class.java,
                                org.ostelco.at.okhttp.SourceTest::class.java,
                                org.ostelco.at.okhttp.PurchaseTest::class.java,
                                org.ostelco.at.okhttp.GraphQlTests::class.java,
                                org.ostelco.at.okhttp.PlanTest::class.java,
                                org.ostelco.at.jersey.CustomerTest::class.java,
                                // org.ostelco.at.jersey.SubscriptionsTest::class.java,
                                org.ostelco.at.jersey.RegionsTest::class.java,
                                org.ostelco.at.jersey.SingaporeKycTest::class.java,
                                org.ostelco.at.jersey.GetProductsTest::class.java,
                                org.ostelco.at.jersey.BundlesAndPurchasesTest::class.java,
                                org.ostelco.at.jersey.SourceTest::class.java,
                                org.ostelco.at.jersey.PurchaseTest::class.java,
                                org.ostelco.at.jersey.PlanTest::class.java,
                                org.ostelco.at.jersey.GraphQlTests::class.java,
                                org.ostelco.at.jersey.JumioKycTest::class.java,
                                org.ostelco.at.pgw.OcsTest::class.java,
                                org.ostelco.at.simmanager.SimManager::class.java
                        )
                )
            }
        }
    }

    /**
     * Test of recurring payments requires that:
     *    1) The serveo.net service docker instance is running.
     *    2) The STRIPE_ENDPOINT_SECRET env. variable set.
     * Note that if the STRIPE_ENDPOINT_SECRET variable is not set, then the test
     * will be skipped altogether.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "STRIPE_ENDPOINT_SECRET", matches="\\S+")
    fun `run recurring payment tests`() {
        runBlocking {

            launch {
                try {
                    StripePayment.enableWebhook()

                    checkResult(
                            JUnitCore.runClasses(
                                    ParallelComputer(true, true),
                                    org.ostelco.at.okhttp.RenewPlanTest::class.java,
                                    org.ostelco.at.jersey.RenewPlanTest::class.java
                            )
                    )
                } finally {
                    StripePayment.enableWebhook(false)
                }
            }
        }
    }

    private fun checkResult(result: org.junit.runner.Result) {

        println()
        println("Test result: ${result.runCount - result.failureCount} of ${result.runCount} tests passed. ${result.ignoreCount} marked to be ignored.")

        result.failures.forEach {
            logger.error("{} {} {} {}", it.testHeader, it.message, it.description, it.trace)
        }

        assertTrue(result.wasSuccessful(), "${result.failureCount} tests failed!")
    }
}