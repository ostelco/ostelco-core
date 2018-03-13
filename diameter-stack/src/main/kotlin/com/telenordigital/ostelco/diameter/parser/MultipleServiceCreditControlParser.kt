package com.telenordigital.ostelco.diameter.parser

// https://tools.ietf.org/html/rfc4006#section-8.16

import com.telenordigital.ostelco.diameter.logger
import com.telenordigital.ostelco.diameter.model.FinalUnitIndication
import com.telenordigital.ostelco.diameter.model.MultipleServiceCreditControl
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet

class MultipleServiceCreditControlParser(serviceControl: AvpSet) : Parser<MultipleServiceCreditControl> {

    private val LOG by logger()

    private var ratingGroup = -1
    private var serviceIdentifier = -1
    private var requestedUnits = 0L
    private var usedUnitsTotal = 0L
    private var usedUnitsInput = 0L
    private var usedUnitsOutput = 0L
    private var grantedServiceUnit = 0L
    private var validityTime = 86400
    private var finalUnitIndication: FinalUnitIndication? = null

    init {
        parseAvps(serviceControl)
    }

    override fun parse(): MultipleServiceCreditControl {
        return MultipleServiceCreditControl(
                ratingGroup,
                serviceIdentifier,
                requestedUnits,
                usedUnitsTotal,
                usedUnitsInput,
                usedUnitsOutput,
                grantedServiceUnit,
                validityTime,
                finalUnitIndication)
    }

    private fun parseAvps(serviceControl: AvpSet) {
        try {
            this.ratingGroup = serviceControl.getAvp(Avp.RATING_GROUP)
                    ?.integer32 ?: -1
            this.serviceIdentifier = serviceControl.getAvp(Avp.SERVICE_IDENTIFIER_CCA)
                    ?.integer32 ?: -1
            this.requestedUnits = serviceControl.getAvp(Avp.REQUESTED_SERVICE_UNIT)
                    ?.grouped
                    ?.getAvp(Avp.CC_TOTAL_OCTETS)
                    ?.unsigned64 ?: 0

            parseUsedServiceUnit(serviceControl)
        } catch (e: AvpDataException) {
            LOG.warn("Failed to parse Multiple-Service-Credit-Control", e)
        }
    }

    private fun parseUsedServiceUnit(serviceControl: AvpSet) {
        try {
            val usedServiceUnitsSet: AvpSet? = serviceControl.getAvps(Avp.USED_SERVICE_UNIT)
            if (usedServiceUnitsSet != null) {
                for (nextUsedServiceUnit in usedServiceUnitsSet) {
                    val usedServiceUnit: AvpSet? = nextUsedServiceUnit.grouped
                    if (usedServiceUnit != null) {
                        this.usedUnitsTotal = usedServiceUnit.getAvp(Avp.CC_TOTAL_OCTETS)
                                ?.unsigned64 ?: 0
                        this.usedUnitsInput = usedServiceUnit.getAvp(Avp.CC_INPUT_OCTETS)
                                ?.unsigned64 ?: 0
                        this.usedUnitsOutput = usedServiceUnit.getAvp(Avp.CC_OUTPUT_OCTETS)
                                ?.unsigned64 ?: 0
                    }
                }
            }
        } catch (e: AvpDataException) {
            LOG.warn("Failed to parse Used-Service-Unit", e)
        }

    }
}
