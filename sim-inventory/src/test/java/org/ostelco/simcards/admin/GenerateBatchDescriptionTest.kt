package org.ostelco.simcards.admin

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.ostelco.simcards.admin.GenerateBatchDescription.Companion.luhnCheck

class GenerateBatchDescriptionTest {


    @Test
    fun testNegativeLuhnCheck() {
        for (x in listOf("79927398710", "79927398711", "79927398712", "79927398714", "79927398715", "79927398716", "79927398717", "79927398718", "79927398719")) {
            assertFalse(luhnCheck(x))
        }
    }

    @Test
    fun testPositiveLuhnCheck() {
        for (x in listOf("79927398713")) {
            assertTrue(luhnCheck(x))
        }
    }

}


class GenerateBatchDescription {
    companion object {
        /**
         * Implement the Luhn algorithm for checksums.  Used when
         * producing valid ICCID numbers.
         * https://en.wikipedia.org/wiki/Luhn_algorithm
         */
        fun luhnCheck(ccNumber: String): Boolean {
            var sum = 0
            var alternate = false
            for (i in ccNumber.length - 1 downTo 0) {
                var n = Integer.parseInt(ccNumber.substring(i, i + 1))
                if (alternate) {
                    n *= 2
                    if (n > 9) {
                        n = n % 10 + 1
                    }
                }
                sum += n
                alternate = !alternate
            }
            return sum % 10 == 0
        }
    }
}