package org.ostelco.simcards.admin

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.ostelco.simcards.IccidBasis


// XXX This code should be deleted.  The functionality in it should either  be moved to
//     a helper script (in Go perhaps), or moved into an as of yet more generic sim batch
//     lifecycle maintenance module.  It shouldn't be part of "test" gode.
class GenerateBatchDescriptionTest {


    @Test
    fun testGenerateIccid() {
        val iccid = IccidBasis(cc = 47, serialNumber = 1)
        val iccidString = iccid.asIccid()
        assertEquals(19, iccidString.length)
    }

    @Test
    fun funPrettyPrintBatchDescription() {
        val iccid = IccidBasis(cc = 47, serialNumber = 1).asIccid()

        val batch = SimBatchDescription(
                customer = "FooTel",
                profileType = "FooTelStd",
                orderDate = "20181212",
                batchNo = 1,
                quantity = 1,
                iccidStart = iccid,
                imsiStart = "4201710010000",
                opKeyLabel = "FooTel-OP",
                transportKeyLabel = "FooTel-TK-1"
        )

        val pp = prettyPrintSimBatchDescription(batch)
        println(pp)
        assertTrue(pp.length > 100)
    }


    private fun prettyPrintSimBatchDescription(bd: SimBatchDescription): String {
        return """
            *HEADER DESCRIPTION
            ***************************************
            Customer        : ${bd.customer}
            ProfileType     : ${bd.profileType}
            Order Date      : ${bd.orderDate}
            Batch No        : ${bd.orderDate}${bd.batchNo}
            Quantity        : ${bd.quantity}
            OP Key label    :
            Transport Key   :
            ***************************************
            *INPUT VARIABLES
            ***************************************
            var_In:
            ICCID: ${bd.iccidStart}
            IMSI: ${bd.imsiStart}
            ***************************************
            *OUTPUT VARIABLES
            ***************************************
            var_Out: ICCID/IMSI/KI
            """.trimIndent()
    }
}


// TODO: This is just a first iteration, things like dates etc. should be
//       not be represented using strings but proper time-objects, but we'll do this for now
//       just too get going.

class SimBatchDescription(
        val customer: String,
        val profileType: String,
        val orderDate: String,
        val batchNo: Int,
        val quantity: Int,
        val iccidStart: String,
        val imsiStart: String,
        val opKeyLabel: String,
        val transportKeyLabel: String)
