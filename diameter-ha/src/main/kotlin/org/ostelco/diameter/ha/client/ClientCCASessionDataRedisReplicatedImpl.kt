package org.ostelco.diameter.ha.client

import org.jdiameter.api.ApplicationId
import org.jdiameter.api.Request
import org.jdiameter.client.impl.app.cca.IClientCCASessionData
import org.jdiameter.common.api.app.cca.ClientCCASessionState
import org.ostelco.diameter.ha.common.AppSessionDataRedisReplicatedImpl
import java.io.Serializable

class ClientCCASessionDataReplicatedImpl(sessionId: String) : AppSessionDataRedisReplicatedImpl(), IClientCCASessionData {
    override fun getTxTimerRequest(): Request {
        TODO("not implemented")
    }

    override fun setGatheredCCFH(gatheredCCFH: Int) {
        TODO("not implemented")
    }

    override fun setGatheredRequestedAction(gatheredRequestedAction: Int) {
        TODO("not implemented")
    }

    override fun isEventBased(): Boolean {
        TODO("not implemented")
    }

    override fun getGatheredDDFH(): Int {
        TODO("not implemented")
    }

    override fun isRequestTypeSet(): Boolean {
        TODO("not implemented")
    }

    override fun setRequestTypeSet(b: Boolean) {
        TODO("not implemented")
    }

    override fun getGatheredRequestedAction(): Int {
        TODO("not implemented")
    }

    override fun setTxTimerRequest(txTimerRequest: Request?) {
        TODO("not implemented")
    }

    override fun getGatheredCCFH(): Int {
        TODO("not implemented")
    }

    override fun setClientCCASessionState(state: ClientCCASessionState?) {
        TODO("not implemented")
    }

    override fun getTxTimerId(): Serializable {
        TODO("not implemented")
    }

    override fun setBuffer(buffer: Request?) {
        TODO("not implemented")
    }

    override fun getBuffer(): Request {
        TODO("not implemented")
    }

    override fun setGatheredDDFH(gatheredDDFH: Int) {
        TODO("not implemented")
    }

    override fun setTxTimerId(txTimerId: Serializable?) {
        TODO("not implemented")
    }

    override fun setEventBased(b: Boolean) {
        TODO("not implemented")
    }

    override fun getClientCCASessionState(): ClientCCASessionState {
        TODO("not implemented")
    }
}