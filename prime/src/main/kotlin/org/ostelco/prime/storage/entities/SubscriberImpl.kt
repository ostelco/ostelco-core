package org.ostelco.prime.storage.entities


class SubscriberImpl : Subscriber {
    private var _msisdn: String? = null
    private var _noOfBytesLeft: Long = 0

    fun setNoOfBytesLeft(noOfBytesLeft: Long) {
        _noOfBytesLeft = noOfBytesLeft
    }

    fun setMsisdn(msisdn: String) {
        _msisdn = msisdn
    }

    override val noOfBytesLeft: Long
        get() = _noOfBytesLeft

    override val msisdn: String?
        get() = _msisdn
}
