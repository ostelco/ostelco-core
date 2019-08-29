package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.ResourceHelpers
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.ostelco.simcards.IccidBasis
import java.io.FileInputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets


class SimCardBatchDescriptorReaderTest {

    private val smdpInputCsvPath: String? =
            ResourceHelpers.resourceFilePath("fixtures/sample-sim-batch-for-sm-dp+.csv")

    @Test
    fun testReadingListOfEntriesFromFile() {
        var foo = 0
        SmDpSimEntryIterator(FileInputStream(smdpInputCsvPath)).forEach { _ -> foo++ }
        assertEquals(100, foo)
    }

    /**
     * This is not a test, it is utility code that is used to generate the input file
     * for for sm-dp+ test article, so ordinarily this "test" should be ignored,
     * but when new testdata needs to be generated, it should be un-ignored, and run,
     * then the generated data should be copied to wherever it should be stored, and
     * ordinary testing can continue.
     *
     * XXX Take this out of the test code, make it into an utility app that can
     *     be easily run from the command line.
     */
    @Test
    @Ignore
    fun generateSmdpInputCsv() {
        val mcc = 310
        val mnc = 150
        val imsiGen   = ImsiGenerator(mcc = mcc, mnc = mnc, msinStart = 0 )
        val iccidGen  = IccidGenerator(startSerialNum = 0)
        val profileName = "FooTel_STD"

        PrintWriter("sample-sim-batch-for-sm-dp+.csv", StandardCharsets.UTF_8).use { writer ->
            writer.println("IMSI, ICCID, PROFILE")
            for (i in 1..100) {
                val imsi = imsiGen.next()
                val iccid = iccidGen.next()
                writer.println("%s,%s,%s".format(imsi, iccid, profileName))
            }
        }
    }
}

class ImsiGenerator(val mcc : Int, val mnc: Int, val msinStart : Int) : Iterator<String> {

    private var msin = msinStart

    @Throws(NoSuchElementException::class)
    override fun next(): String {
        return "%03d%02d%010d".format(mcc, mnc, msin++)
    }

    override  fun hasNext(): Boolean {
        return true
    }
}

class IccidGenerator(val startSerialNum: Int = 0) : Iterator<String> {

    private var serialNumber:Int = startSerialNum
    /**
     * Returns the next element in the iteration.
     */
    @Throws(NoSuchElementException::class)
    override fun next(): String {
         return IccidBasis(serialNumber = serialNumber++).asIccid()
    }

    override  fun hasNext(): Boolean {
        return true
    }
}