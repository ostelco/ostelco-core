package org.ostelco.simcards.smdpplus

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * Read a CSV input stream containing simulated input to an SM-DP+, with columns
 * ICCID, IMSI and Profile.  Return an iterator over SmDpSimEntry instances.
 */
class SmDpSimEntryIterator(csvInputStream: InputStream) : Iterator<SmDpSimEntry> {

    private val log = LoggerFactory.getLogger(javaClass)

    private var count = AtomicLong(0)

    private val values = ConcurrentLinkedDeque<SmDpSimEntry>()

    init {

        val csvFileFormat = CSVFormat.DEFAULT
                .withQuote(null)
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines(true)
                .withTrim()
                .withDelimiter(',')

        BufferedReader(InputStreamReader(csvInputStream, Charset.forName(
                "ISO-8859-1"))).use { reader ->
            CSVParser(reader, csvFileFormat).use { csvParser ->
                for (record in csvParser) {
                    val iccid = record.get("ICCID")
                    val imsi = record.get("IMSI")
                    val profile = record.get("PROFILE")

                    val value = SmDpSimEntry(
                            iccid = iccid,
                            imsi = imsi,
                            profile = profile)

                    values.add(value)
                    count.incrementAndGet()
                }
            }
        }
    }

    /**
     * Returns the next element in the iteration.
     */
    override operator fun next(): SmDpSimEntry {
        return values.removeLast()
    }

    /**
     * Returns `true` if the iteration has more elements.
     */
    override operator fun hasNext(): Boolean {
        return !values.isEmpty()
    }
}
