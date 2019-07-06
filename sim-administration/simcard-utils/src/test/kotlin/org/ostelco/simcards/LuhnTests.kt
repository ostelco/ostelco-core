package org.ostelco.simcards

import junit.framework.TestCase
import org.junit.Test
import org.ostelco.simcards.LuhnChecksum.luhnCheck
import org.ostelco.simcards.LuhnChecksum.luhnComplete


class LuhnTests {

    @Test
    fun testNegativeLuhnCheck() {
        for (x in listOf(
                "79927398710",
                "79927398711",
                "79927398712",
                "79927398714",
                "79927398715",
                "79927398716",
                "79927398717",
                "79927398718",
                "79927398719")) {
            TestCase.assertFalse(luhnCheck(x))
        }
    }

    @Test
    fun testPositiveLuhnCheck() {
        for (x in listOf("79927398713")) {
            TestCase.assertTrue(luhnCheck(x))
        }
    }

    @Test
    fun testLuhnComplete() {
        TestCase.assertTrue(luhnCheck(luhnComplete("79927398710")))
    }
}