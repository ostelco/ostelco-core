package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.ResourceHelpers
import junit.framework.Assert.assertEquals
import org.junit.Test
import java.io.FileInputStream


class SimCarfBatchDescriptorReaderTest {

    val csvPath =
            ResourceHelpers.resourceFilePath("fixtures/sample-sim-csv-file.csv")
    @Test
    fun testReadingListOfEntriesFromFile() {
        var foo = 0
        SmDpSimEntryIterator(FileInputStream(csvPath)).forEach {  foo++}
        assertEquals(4, foo)
    }
}
