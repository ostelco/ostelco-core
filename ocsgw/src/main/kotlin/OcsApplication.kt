package org.ostelco.ocsgw

import OcsServer
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.StorageOptions
import org.jdiameter.api.*
import org.jdiameter.api.Stack
import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.api.cca.events.JCreditControlRequest
import org.jdiameter.client.api.ISessionFactory
import org.jdiameter.common.impl.app.cca.CCASessionFactoryImpl
import org.jdiameter.server.impl.StackImpl
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl
import org.jdiameter.server.impl.helpers.XMLConfiguration
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.model.RequestType.getTypeAsString
import org.ostelco.ocsgw.utils.AppConfig
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

class OcsApplication : CCASessionFactoryImpl(), NetworkReqListener {
    private fun fetchConfig(configDir: String?, configFile: String?) {
        val vpcEnv = System.getenv("VPC_ENV")
        val instance = System.getenv("INSTANCE")
        val serviceFile = System.getenv("SERVICE_FILE")
        if (vpcEnv != null && instance != null) {
            val bucketName = "ocsgw-$vpcEnv-$instance-bucket"
            fetchFromStorage(configFile, configDir, bucketName)
            fetchFromStorage(serviceFile, configDir, bucketName)
        }
    }

    private fun fetchFromStorage(fileName: String?, configDir: String?, bucketName: String) {
        if (fileName == null) {
            return
        }
        LOG.debug("Downloading file : $fileName")
        val storage = StorageOptions.getDefaultInstance().service
        val blobFile = storage[BlobId.of(bucketName, fileName)]
        val destFilePath = Paths.get("$configDir/$fileName")
        blobFile.downloadTo(destFilePath)
    }

    fun start(configDir: String?, configFile: String?) {
        try {
            fetchConfig(configDir, configFile)
            val diameterConfig: Configuration = XMLConfiguration(configDir + configFile)
            stack = StackImpl()
            sessionFactory = stack.init(diameterConfig) as ISessionFactory
            OcsServer.init(stack, AppConfig())
            val network = stack.unwrap(Network::class.java)
            network.addNetworkReqListener(this, ApplicationId.createByAuthAppId(0L, APPLICATION_ID))
            network.addNetworkReqListener(this, ApplicationId.createByAuthAppId(VENDOR_ID_3GPP, APPLICATION_ID))
            stack.start(Mode.ALL_PEERS, 30000, TimeUnit.MILLISECONDS)
            init(sessionFactory)
            sessionFactory.registerAppFacory(ServerCCASession::class.java, this)
            printAppIds()
        } catch (e: Exception) {
            LOG.error("Failure initializing OcsApplication", e)
        }
    }

    override fun processRequest(request: Request): Answer? {
        LOG.debug("[<<] Received Request [{}]", request.sessionId)
        try {
            val session = sessionFactory.getNewAppSession<ServerCCASessionImpl>(request.sessionId, ApplicationId.createByAuthAppId(4L), ServerCCASession::class.java)
            session.processRequest(request)
            LOG.debug("processRequest finished [{}]", request.sessionId)
        } catch (e: InternalException) {
            LOG.error("[><] Failure handling received request.", e)
        }
        return null
    }

    override fun doCreditControlRequest(session: ServerCCASession, request: JCreditControlRequest) {
        when (request.requestTypeAVPValue) {
            RequestType.INITIAL_REQUEST, RequestType.UPDATE_REQUEST, RequestType.TERMINATION_REQUEST -> {
                LOG.info("[<<] Received Credit-Control-Request from P-GW [ {} ] [{}]", getTypeAsString(request.requestTypeAVPValue), session.sessionId)
                try {
                    OcsServer.handleRequest(session, request)
                } catch (e: Exception) {
                    LOG.error("[><] Failure processing Credit-Control-Request [" + getTypeAsString(request.requestTypeAVPValue) + "] + [session.getSessionId()]", e)
                }
            }
            RequestType.EVENT_REQUEST -> LOG.info("[<<] Received Credit-Control-Request [EVENT]")
            else -> {
            }
        }
    }

    private fun printAppIds() {
        val appIds = stack.metaData.localPeer.commonApplications
        LOG.info("Diameter Stack  :: Supporting {} applications.", appIds.size)
        for (id in appIds) {
            LOG.info("Diameter Stack  :: Common :: {}", id)
        }
        LOG.info("Uri : " + stack.metaData.localPeer.uri)
        LOG.info("Realm : " + stack.metaData.localPeer.realmName)
        LOG.info("IP : " + Arrays.toString(stack.metaData.localPeer.ipAddresses))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OcsApplication::class.java)
        private const val DIAMETER_CONFIG_FILE = "server-jdiameter-config.xml"
        private const val CONFIG_FOLDER = "/config/"
        private const val APPLICATION_ID = 4L // Diameter Credit Control Application (4)
        private const val VENDOR_ID_3GPP: Long = 10415
        private lateinit var stack : Stack
        @JvmStatic
        fun main(args: Array<String>) {
            Runtime.getRuntime().addShutdownHook(Thread(Runnable { shutdown() }))
            val app = OcsApplication()
            var configFile = System.getenv("DIAMETER_CONFIG_FILE")
            if (configFile == null) {
                configFile = DIAMETER_CONFIG_FILE
            }
            var configFolder = System.getenv("CONFIG_FOLDER")
            if (configFolder == null) {
                configFolder = CONFIG_FOLDER
            }
            app.start(configFolder, configFile)
        }

        fun shutdown() {
            LOG.info("Shutting down OcsApplication...")
            try {
                stack.stop(30000, TimeUnit.MILLISECONDS, DisconnectCause.REBOOTING)
            } catch (e: IllegalDiameterStateException) {
                LOG.error("Failed to gracefully shutdown OcsApplication", e)
            } catch (e: InternalException) {
                LOG.error("Failed to gracefully shutdown OcsApplication", e)
            }
            stack.destroy()
        }
    }
}