package com.telenordigital.ostelco.diameter.parser


import com.telenordigital.ostelco.diameter.logger
import com.telenordigital.ostelco.diameter.model.PsInformation
import com.telenordigital.ostelco.diameter.model.ServiceInformation
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet

class ServiceInformationParser(val serviceInformationAvp: Avp) : Parser<ServiceInformation> {

    private val LOG by logger()

    private var psInformation: PsInformation? = null

    init {
        parseApvs(serviceInformationAvp)
    }

    override fun parse(): ServiceInformation {
        return ServiceInformation(psInformation)
    }

    private fun parseApvs(serviceInformationAvp: Avp) {
        try {
            val psInformationAvps: AvpSet? = serviceInformationAvp.grouped.getAvp(Avp.PS_INFORMATION)?.grouped
            if (psInformationAvps != null) {
                psInformation = PsInformationParser(psInformationAvps).parse()
            } else {
                LOG.info("No PS-Information")
            }
        } catch (e: AvpDataException) {
            LOG.error("Failed to parse Service-Information", e)
        }
    }
}
