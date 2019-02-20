package org.ostelco.at

import org.junit.Test
import org.junit.experimental.ParallelComputer
import org.junit.runner.JUnitCore

class TestSuite {

    @Test
    fun `run all tests in parallel`() {

        JUnitCore.runClasses(
                ParallelComputer(true, true),
                org.ostelco.at.okhttp.GetProductsTest::class.java,
                org.ostelco.at.okhttp.BundlesAndPurchasesTest::class.java,
                org.ostelco.at.okhttp.SourceTest::class.java,
                org.ostelco.at.okhttp.PurchaseTest::class.java,
                org.ostelco.at.okhttp.CustomerTest::class.java,
                org.ostelco.at.okhttp.GraphQlTests::class.java,
                org.ostelco.at.jersey.GetProductsTest::class.java,
                org.ostelco.at.jersey.BundlesAndPurchasesTest::class.java,
                org.ostelco.at.jersey.SourceTest::class.java,
                org.ostelco.at.jersey.PurchaseTest::class.java,
                org.ostelco.at.jersey.PlanTest::class.java,
                org.ostelco.at.jersey.CustomerTest::class.java,
                org.ostelco.at.jersey.GraphQlTests::class.java,
                org.ostelco.at.jersey.eKYCTest::class.java,
                org.ostelco.at.pgw.OcsTest::class.java)
    }
}