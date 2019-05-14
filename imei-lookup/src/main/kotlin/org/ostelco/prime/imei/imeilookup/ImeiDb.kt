package org.ostelco.prime.imei.imeilookup

import arrow.core.Either
import org.ostelco.prime.getLogger
import org.ostelco.prime.imei.ImeiLookup
import org.ostelco.prime.imei.core.BadRequestError
import org.ostelco.prime.imei.core.Imei
import org.ostelco.prime.imei.core.ImeiLookupError
import org.ostelco.prime.imei.core.ImeiNotFoundError
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException


/**
 *  In memory implementation of the IMEI lookup service
 */
class ImeiDb : ImeiLookup by ImeiDdSingleton

object ImeiDdSingleton : ImeiLookup {

    private val TAC_IDX = 0
    private val MARKETING_NAME_IDX = 1
    private val MANUFACTURER_IDX = 2
    private val BRAND_NAME_IDX = 9
    private val MODEL_NAME_IDX = 10
    private val OPERATING_SYSTEM_IDX = 11
    private val DEVICE_TYPE_IDX = 15
    private val OEM_IDX = 16

    private val logger by getLogger()

    private val db = HashMap<String, Imei>()

    override fun getImeiInformation(imei: String): Either<ImeiLookupError, Imei> {

        if (!(15 <= imei.length) && (imei.length <= 16)) {
            return Either.left(BadRequestError("Malformed IMEI. Size should be 15 digit for IMEI or 16 digit for IMEISV"))
        }

        val tac = imei.substring(0, 8)

        val imeiInformation = db[tac]
        if (imeiInformation != null) {
            return Either.right(imeiInformation)
        }
        return Either.left(ImeiNotFoundError("Not implemented jet"))
    }

    fun loadFile(fileName: String): Either<ImeiLookupError, Boolean> {
        logger.info("Loading file $fileName")

        var fileReader: BufferedReader? = null

        try {
            fileReader = BufferedReader(FileReader(fileName))

            // Read CSV header
            fileReader.readLine()

            var line = fileReader.readLine()
            while (line != null) {
                val tokens = line.split("|")
                if (tokens.size > 0) {
                    val imei = Imei(
                            tokens[TAC_IDX],
                            tokens[MARKETING_NAME_IDX],
                            tokens[MANUFACTURER_IDX],
                            tokens[BRAND_NAME_IDX],
                            tokens[MODEL_NAME_IDX],
                            tokens[OPERATING_SYSTEM_IDX],
                            tokens[DEVICE_TYPE_IDX],
                            tokens[OEM_IDX])
                    db.put(imei.tac, imei)
                }
                line = fileReader.readLine()
            }
        } catch (e: Exception) {
            logger.error("Reading CSV Error!", e)
        } finally {
            try {
                fileReader!!.close()
            } catch (e: IOException) {
                logger.error("Closing fileReader Error!", e)
            }
        }
        return Either.right(true)
    }
}
