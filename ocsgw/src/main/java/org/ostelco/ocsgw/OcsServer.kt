package org.ostelco.ocsgw

import org.jdiameter.api.ApplicationId
import org.jdiameter.api.Avp
import org.jdiameter.api.IllegalDiameterStateException
import org.jdiameter.api.InternalException
import org.jdiameter.api.OverloadException
import org.jdiameter.api.RouteException
import org.jdiameter.api.Stack
import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.api.cca.events.JCreditControlRequest
import org.jdiameter.common.impl.app.auth.ReAuthRequestImpl
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl
import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.getLogger
import org.ostelco.diameter.model.ReAuthRequestType
import org.ostelco.diameter.model.SessionContext
import org.ostelco.ocsgw.datasource.DataSource
import org.ostelco.ocsgw.datasource.DataSourceType.Local
import org.ostelco.ocsgw.datasource.DataSourceType.Proxy
import org.ostelco.ocsgw.datasource.DataSourceType.PubSub
import org.ostelco.ocsgw.datasource.DataSourceType.gRPC
import org.ostelco.ocsgw.datasource.SecondaryDataSourceType
import org.ostelco.ocsgw.datasource.local.LocalDataSource
import org.ostelco.ocsgw.datasource.protobuf.GrpcDataSource
import org.ostelco.ocsgw.datasource.protobuf.ProtobufDataSource
import org.ostelco.ocsgw.datasource.protobuf.PubSubDataSource
import org.ostelco.ocsgw.datasource.proxy.ProxyDataSource
import org.ostelco.ocsgw.utils.AppConfig


object OcsServer {

    private val logger by getLogger()

    var stack: Stack? = null
        private set

    private var source: DataSource? = null
    private var localPeerFQDN: String? = null
    private var localPeerRealm: String? = null

    internal fun handleRequest(session: ServerCCASession, request: JCreditControlRequest) {
        try {
            val ccrContext = CreditControlContext(
                    session.sessionId,
                    request,
                    localPeerFQDN!!,
                    localPeerRealm!!
            )
            source?.handleRequest(ccrContext) ?: logger.error("Received request before initialising stack")
        } catch (e: Exception) {
            logger.error("Failed to create CreditControlContext", e)
        }
    }

    //https://tools.ietf.org/html/rfc4006#page-30
    //https://tools.ietf.org/html/rfc3588#page-101
    fun sendReAuthRequest(sessionContext: SessionContext?) {
        try {
            val ccaSession = stack?.getSession(sessionContext?.sessionId, ServerCCASessionImpl::class.java)
            if (ccaSession != null && ccaSession.isValid) {
                // TODO martin: Not sure why there are multiple sessions for one session Id.
                for (session in ccaSession.sessions) {
                    if (session.isValid) {
                        val request = session.createRequest(258,
                                ApplicationId.createByAuthAppId(4L),
                                sessionContext?.originRealm,
                                sessionContext?.originHost
                        )
                        val avps = request.avps
                        avps.addAvp(Avp.RE_AUTH_REQUEST_TYPE, ReAuthRequestType.AUTHORIZE_ONLY.ordinal, true, false)
                        val reAuthRequest = ReAuthRequestImpl(request)
                        ccaSession.sendReAuthRequest(reAuthRequest)
                    } else {
                        logger.info("Invalid session")
                    }
                }
            } else {
                logger.info("No session with ID {}", sessionContext?.sessionId)
            }
        } catch (e: InternalException) {
            logger.warn("Failed to send Re-Auth Request", e)
        } catch (e: IllegalDiameterStateException) {
            logger.warn("Failed to send Re-Auth Request", e)
        } catch (e: RouteException) {
            logger.warn("Failed to send Re-Auth Request", e)
        } catch (e: OverloadException) {
            logger.warn("Failed to send Re-Auth Request", e)
        }

    }

    internal fun init(stack: Stack, appConfig: AppConfig) {
        this.stack = stack
        this.localPeerFQDN = stack.metaData.localPeer.uri.fqdn
        this.localPeerRealm = stack.metaData.localPeer.realmName

        val protobufDataSource = ProtobufDataSource()

        source = when (appConfig.dataStoreType) {
            Proxy -> {
                logger.info("Using ProxyDataSource")
                val secondary = when (appConfig.secondaryDataStoreType) {
                    SecondaryDataSourceType.PubSub -> getPubSubDataSource(protobufDataSource, appConfig)
                    SecondaryDataSourceType.gRPC -> getGrpcDataSource(protobufDataSource, appConfig)
                    else -> getPubSubDataSource(protobufDataSource, appConfig)
                }
                secondary.init()
                ProxyDataSource(secondary)
            }
            Local -> {
                logger.info("Using LocalDataSource")
                LocalDataSource()
            }
            PubSub -> {
                logger.info("Using PubSubDataSource")
                getPubSubDataSource(protobufDataSource, appConfig)
            }
            gRPC -> {
                logger.info("Using GrpcDataSource")
                getGrpcDataSource(protobufDataSource, appConfig)
            }
            else -> {
                logger.warn("Unknown DataStoreType {}", appConfig.dataStoreType)
                LocalDataSource()
            }
        }
        source?.init()
    }

    private fun getGrpcDataSource(
            protobufDataSource: ProtobufDataSource,
            appConfig: AppConfig): GrpcDataSource =
            GrpcDataSource(
                    protobufDataSource,
                    appConfig.grpcServer,
                    appConfig.metricsServer)

    private fun getPubSubDataSource(
            protobufDataSource: ProtobufDataSource,
            appConfig: AppConfig): PubSubDataSource =
            PubSubDataSource(protobufDataSource,
                    appConfig.pubSubProjectId,
                    appConfig.pubSubTopicId,
                    appConfig.pubSubSubscriptionIdForCcr,
                    appConfig.pubSubSubscriptionIdForActivate)
}