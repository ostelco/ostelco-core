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
import org.ostelco.ocsgw.datasource.DataSourceType
import org.ostelco.ocsgw.datasource.DataSourceType.Local
import org.ostelco.ocsgw.datasource.DataSourceType.Proxy
import org.ostelco.ocsgw.datasource.DataSourceType.PubSub
import org.ostelco.ocsgw.datasource.DataSourceType.gRPC
import org.ostelco.ocsgw.datasource.DataSourceType.Multi
import org.ostelco.ocsgw.datasource.local.LocalDataSource
import org.ostelco.ocsgw.datasource.multi.MultiDataSource
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
    private var defaultRequestedServiceUnit: Long = 0L

    internal fun handleRequest(session: ServerCCASession, request: JCreditControlRequest) {

        val peerFqdn = localPeerFQDN
        if (peerFqdn == null) {
            logger.error("Failed to create CreditControlContext, local peer fqdn is null")
            return
        }

        val peerRealm = localPeerRealm
        if (peerRealm == null) {
            logger.error("Failed to create CreditControlContext, local peer realm is null")
            return
        }

        try {
            val ccrContext = CreditControlContext(
                    session.sessionId,
                    request,
                    peerFqdn,
                    peerRealm
            )
            setDefaultRequestedServiceUnit(ccrContext)
            source?.handleRequest(ccrContext) ?: logger.error("Received request before initialising stack")
        } catch (e: Exception) {
            logger.error("Failed to create CreditControlContext", e)
        }
    }

    // In the case where the Diameter client does not set the Requested-Service-Unit AVP
    // in the Multiple-Service-Credit-Control we need to set a default value.
    private fun setDefaultRequestedServiceUnit(ccrContext: CreditControlContext) {
        ccrContext.creditControlRequest.multipleServiceCreditControls.forEach { mscc ->
                if ( mscc.requested.size == 1 ) {
                    if ( mscc.requested.get(0).total <= 0) {
                        mscc.requested.get(0).total = defaultRequestedServiceUnit
                    }
                }
            }
    }

    //https://tools.ietf.org/html/rfc4006#page-30
    //https://tools.ietf.org/html/rfc3588#page-101
    fun sendReAuthRequest(sessionContext: SessionContext?) {
        try {
            val ccaSession = stack?.getSession(sessionContext?.sessionId, ServerCCASessionImpl::class.java)
            if (ccaSession != null && ccaSession.isValid) {
                for (session in ccaSession.sessions) {
                    if (session.isValid) {
                        val request = session.createRequest(258,
                                ApplicationId.createByAuthAppId(4L),
                                sessionContext?.originRealm,
                                sessionContext?.originHost
                        )
                        request.isProxiable = true
                        val avps = request.avps
                        avps.addAvp(Avp.RE_AUTH_REQUEST_TYPE, ReAuthRequestType.AUTHORIZE_ONLY.ordinal, true, false)
                        val reAuthRequest = ReAuthRequestImpl(request)
                        ccaSession.sendReAuthRequest(reAuthRequest)
                        logger.debug("Sent RAR [{}]", sessionContext?.sessionId)
                    } else {
                        logger.info("Invalid session")
                    }
                }
            } else {
                logger.info("No session with ID {}", sessionContext?.sessionId)
            }
        } catch (e: InternalException) {
            logger.warn("Failed to send Re-Auth Request [{}]", sessionContext?.sessionId, e)
        } catch (e: IllegalDiameterStateException) {
            logger.warn("Failed to send Re-Auth Request [{}]", sessionContext?.sessionId, e)
        } catch (e: RouteException) {
            logger.warn("Failed to send Re-Auth Request [{}]", sessionContext?.sessionId, e)
        } catch (e: OverloadException) {
            logger.warn("Failed to send Re-Auth Request [{}]", sessionContext?.sessionId, e)
        }

    }

    internal fun init(stack: Stack, appConfig: AppConfig) {
        this.stack = stack
        this.localPeerFQDN = stack.metaData.localPeer.uri.fqdn
        this.localPeerRealm = stack.metaData.localPeer.realmName

        this.defaultRequestedServiceUnit = appConfig.defaultRequestedServiceUnit

        source = setupDataSource(appConfig.dataStoreType, appConfig)
        source?.init()
    }

    private fun setupDataSource(dataSourceType: DataSourceType, appConfig: AppConfig) : DataSource {
        val protobufDataSource = ProtobufDataSource()
        return when (dataSourceType) {
            Proxy -> {
                logger.info("Using ProxyDataSource")
                setupProxyDataSource(protobufDataSource, appConfig)
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
            Multi -> {
                logger.info("Using MultiDataSource")
                setupMultiDataSource(appConfig)
            }
        }
    }

    private fun setupProxyDataSource(protobufDataSource: ProtobufDataSource, appConfig: AppConfig) : DataSource {
        val secondary = when (appConfig.secondaryDataSourceType) {
            PubSub -> {
                logger.info("SecondaryDataStore set to PubSub")
                getPubSubDataSource(protobufDataSource, appConfig)
            }
            gRPC -> {
                logger.info("SecondaryDataStore set to gRPC")
                getGrpcDataSource(protobufDataSource, appConfig)
            }
            else -> {
                logger.info("Default SecondaryDataSource PubSub")
                getPubSubDataSource(protobufDataSource, appConfig)
            }
        }
        secondary.init()
        return ProxyDataSource(secondary)
    }

    private fun setupMultiDataSource(appConfig: AppConfig) : DataSource {
        val initDataSource = setupDataSource(appConfig.getMultiInitDataSourceType(), appConfig)
        val updateDataSource = setupDataSource(appConfig.getMultiUpdateDataSourceType(), appConfig)
        val terminateDataSource = setupDataSource(appConfig.getMultiTerminateDataSourceType(), appConfig)
        return MultiDataSource(initDataSource, updateDataSource, terminateDataSource)
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
                    appConfig.pubSubTopicIdForCcr,
                    appConfig.pubSubTopicIdForCca,
                    appConfig.pubSubSubscriptionIdForCca,
                    appConfig.pubSubSubscriptionIdForActivate)
}