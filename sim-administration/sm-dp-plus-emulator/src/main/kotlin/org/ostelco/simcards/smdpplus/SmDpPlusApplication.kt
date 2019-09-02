package org.ostelco.simcards.smdpplus

import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.dropwizardutils.CertAuthConfig
import org.ostelco.dropwizardutils.CertificateAuthorizationFilter
import org.ostelco.dropwizardutils.OpenapiResourceAdder.Companion.addOpenapiResourceToJerseyEnv
import org.ostelco.dropwizardutils.OpenapiResourceAdderConfig
import org.ostelco.dropwizardutils.RBACService
import org.ostelco.dropwizardutils.RolesConfig
import org.ostelco.sim.es2plus.ES2NotificationPointStatus
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter.Companion.addEs2PlusDefaultFiltersAndInterceptors
import org.ostelco.sim.es2plus.Es2ConfirmOrderResponse
import org.ostelco.sim.es2plus.Es2DownloadOrderResponse
import org.ostelco.sim.es2plus.Es2ProfileStatusResponse
import org.ostelco.sim.es2plus.EsTwoPlusConfig
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.sim.es2plus.SmDpPlusServerResource
import org.ostelco.sim.es2plus.SmDpPlusService
import org.ostelco.sim.es2plus.eS2SuccessResponseHeader
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response


fun main(args: Array<String>) = SmDpPlusApplication().run(*args)

/**
 * NOTE: This is not a proper SM-DP+ application, it is a test fixture
 * to be used when acceptance-testing the sim administration application.
 *
 * The intent of the SmDpPlusApplication is to be run in Docker Compose,
 * to serve a few simple ES2+ commands, and to do so consistently, and to
 * report back to the sim administration application via ES2+ callback, as to
 * exercise that part of the protocol as well.
 *
 * In no shape or form is this intended to be a proper SmDpPlus application. It
 * does not store sim profiles, it does not talk ES9+ or ES8+ or indeed do
 * any of the things that would be useful for serving actual eSIM profiles.
 *
 * With those caveats in mind, let's go on to the important task of making a simplified
 * SM-DP+ that can serve as a test fixture :-)
 */
class SmDpPlusApplication : Application<SmDpPlusAppConfiguration>() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getName(): String {
        return "SM-DP+ implementation (partial, only for testing of sim admin service)"
    }

    override fun initialize(bootstrap: Bootstrap<SmDpPlusAppConfiguration>) {
        bootstrap.objectMapper.registerModule(KotlinModule())
    }

    private lateinit var httpClient: CloseableHttpClient

    internal lateinit var es2PlusCLientForCallbacks: ES2PlusClient

    private lateinit var serverResource: SmDpPlusServerResource

    private lateinit var smdpPlusService: SmDpPlusEmulator


    fun noOfEntries () : Int = smdpPlusService.getNoOfEntries()

    fun getHttpClient() = httpClient

    override fun run(config: SmDpPlusAppConfiguration,
                     env: Environment) {

        val jerseyEnvironment = env.jersey()
        this.httpClient = HttpClientBuilder(env).using(config.httpClientConfiguration).build(name)

        // simulate-download-of/iccid/${allocatedProfile.iccid}

        addOpenapiResourceToJerseyEnv(jerseyEnvironment, config.openApi)
        addEs2PlusDefaultFiltersAndInterceptors(jerseyEnvironment)

        log.info("Reading configs from '${config.simBatchData}'")
        if (!File(config.simBatchData).exists()) {
            log.error("Input file '$config.simBatchData' does not exist, bailing out!")
            System.exit(0)
        } else {
            log.info("Input file '$config.simBatchData' does exist, will try to read it!")
        }

        if (!File(config.simBatchData).canRead()) {
            log.error("Input file '$config.simBatchData' can't be read, bailing out!")
            System.exit(0)
        } else {
            log.info("Input file '$config.simBatchData' is readable, will try to read it!")
        }

        val simEntriesIterator = SmDpSimEntryIterator(FileInputStream(config.simBatchData))
        val smDpPlusEmulator = SmDpPlusEmulator(simEntriesIterator)
        this.smdpPlusService = smDpPlusEmulator

        this.serverResource = SmDpPlusServerResource(
                smDpPlus = smdpPlusService)
        jerseyEnvironment.register(serverResource)

        /* jerseyEnvironment.register(CertificateAuthorizationFilter(RBACService(
                rolesConfig = config.rolesConfig,
                certConfig = config.certConfig))) */


        val callbackClient = SmDpPlusCallbackClient(
                httpClient = httpClient,
                hostname = config.es2plusConfig.host,
                portNumber = config.es2plusConfig.port,
                requesterId = config.es2plusConfig.requesterId,
                smdpPlus = smdpPlusService)

        val commandsProcessor = CommandsProcessorResource(callbackClient)
        jerseyEnvironment.register(commandsProcessor)

        // XXX This is weird, is it even necessary?  Probably not.
        jerseyEnvironment.register(CertificateAuthorizationFilter(
                RBACService(rolesConfig = config.rolesConfig,
                        certConfig = config.certConfig)))

        this.es2PlusCLientForCallbacks = ES2PlusClient(
                requesterId = config.es2plusConfig.requesterId,
                host = config.es2plusConfig.host,
                port = config.es2plusConfig.port,
                httpClient = httpClient)

        env.healthChecks().register("coreEmulatorHealthcheck", smDpPlusEmulator.getHealthCheckInstance())

        reset()
    }

    fun reset() {
        this.smdpPlusService.reset();
    }
}


class SmDpPlusCallbackClient(
        val httpClient: HttpClient,
        val hostname: String,
        val portNumber: Int,
        val requesterId: String,
        val smdpPlus: SmDpPlusEmulator) {

    private val log = LoggerFactory.getLogger(javaClass)

    val client: ES2PlusClient

    init {
        this.client = ES2PlusClient(requesterId = requesterId, httpClient = httpClient, host = hostname, port = portNumber)
    }

    @Throws(WebApplicationException::class)
    fun reportDownload(iccid: String) {
        val entry = smdpPlus.getEntryByIccid(iccid)
        if (entry == null) {
            log.error("Attempt to report download for unknown ICCID=$iccid")
            throw WebApplicationException(Response.Status.NOT_FOUND)
        }
        client.handleDownloadProgressInfo(iccid = iccid,
                profileType = entry.profile,
                notificationPointId = 4711, // XXX Obviously a placeholder
                notificationPointStatus = ES2NotificationPointStatus())  // XXX Also a placeholder
    }
}

/**
 * Misc. commands that are useful to give the SM-DP+ outside of its standardized
 * ES2+ commands.   In particular  we add a REST command to simulate an  ES9+ download
 * of a profile.  This command will trigger an ES2+ callback into the prime entity
 * that has been registred to receive the callbacks.
 */
@Path("commands")
class CommandsProcessorResource(private val callbackClient: SmDpPlusCallbackClient) {


    @Path("simulate-download-of/iccid/{iccid}")
    @GET
    fun simulateDownloadOf(@PathParam("iccid") iccid: String): String {
        callbackClient.reportDownload(iccid = iccid)
        return "Simulated download of iccid ${iccid} went well."
    }
}

/**
 * A very reduced  functionality SmDpPlus, essentially handling only
 * happy day scenarios, and not particulary efficient, and in-memory
 * only etc.
 */
class SmDpPlusEmulator(incomingEntries: Iterator<SmDpSimEntry>) : SmDpPlusService {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Global lock, just in case.
     */
    private val entriesLock = Object()

    private val entries: MutableSet<SmDpSimEntry> = mutableSetOf()
    private val entriesByIccid = mutableMapOf<String, SmDpSimEntry>()
    private val entriesByImsi = mutableMapOf<String, SmDpSimEntry>()
    private val entriesByProfile = mutableMapOf<String, MutableSet<SmDpSimEntry>>()

    private val originalEntries: MutableSet<SmDpSimEntry> = mutableSetOf()

    private val healthCheck: HealthCheck = SmDpPlusEmulatorHealthCheck()

    init {
        incomingEntries.forEach { originalEntries.add(it) }

        reset()

        val noOfEntries = entries.size

        if (noOfEntries != 0) {
            log.info("Just read ${noOfEntries} SIM entries.")
        } else {
            log.error("Just read zero SIM entries, this is useless, will abort!")
            System.exit(0)
        }
    }

    inner class SmDpPlusEmulatorHealthCheck() : HealthCheck() {

        @Throws(Exception::class)
        override fun check(): HealthCheck.Result {
            return if (entries.isNotEmpty()) {
                HealthCheck.Result.healthy()
            } else HealthCheck.Result.unhealthy("Has no entries, should have at least one.")
        }
    }

    fun getNoOfEntries () : Int = entries.size

    fun getHealthCheckInstance(): HealthCheck = this.healthCheck


    fun reset() {
        entries.clear()
        entriesByIccid.clear()
        entriesByProfile.clear()
        entriesByImsi.clear()

        originalEntries.map { it.clone() }.forEach {
            entries.add(it)
            entriesByIccid[it.iccid] = it
            entriesByImsi[it.imsi] = it
            val entriesForProfile: MutableSet<SmDpSimEntry>
            if (!entriesByProfile.containsKey(it.profile)) {
                entriesForProfile = mutableSetOf()
                entriesByProfile[it.profile] = entriesForProfile
            } else {
                entriesForProfile = entriesByProfile[it.profile]!!
            }
            entriesForProfile.add(it)
        }

        // Just checking.  This shouldn't happen, but if the original entries were not
        // properly copied by toList, it could heasily happen.
        entries.forEach { if (it.allocated) throw RuntimeException("Already allocated new entry $it") }
    }

    fun getEntryByIccid(iccid: String): SmDpSimEntry? = entriesByIccid[iccid]


    // TODO; What about the reservation flag?
    override fun downloadOrder(eid: String?, iccid: String?, profileType: String?): Es2DownloadOrderResponse {
        synchronized(entriesLock) {
            val entry: SmDpSimEntry = findMatchingFreeProfile(iccid, profileType)
                    ?: throw SmDpPlusException("Could not find download order matching criteria")

            // If an EID is known, then mark this as the IED associated
            // with the entry.
            if (eid != null) {
                entry.eid = eid
            }

            // Then mark the entry as allocated and return the corresponding ICCID.
            entry.allocated = true

            // Finally return the ICCID uniquely identifying the profile instance.
            return Es2DownloadOrderResponse(eS2SuccessResponseHeader(),
                    iccid = entry.iccid)
        }
    }

    /**
     * Find a free profile that either matches both profileStatusList and profile type (if profileStatusList != null),
     * or just profile type (if profileStatusList == null).  Throw runtime exception if parameter
     * errors are discovered, but return null if no matching profile is found.
     */
    private fun findMatchingFreeProfile(iccid: String?, profileType: String?): SmDpSimEntry? {
        return if (iccid != null) {
            findUnallocatedByIccidAndProfileType(iccid, profileType)
        } else if (profileType == null) {
            throw RuntimeException("No profileStatusList, no profile type, so don't know how to allocate sim entry")
        } else if (!entriesByProfile.containsKey(profileType)) {
            throw SmDpPlusException("Unknown profile type $profileType")
        } else {
            allocateByProfile(profileType)
        }
    }

    /**
     * Find an allocatable profile  by profile type.  If a free and matching profile can be found.  If not, then
     * return null.
     */
    private fun allocateByProfile(profileType: String): SmDpSimEntry? {
        val entriesForProfile = entriesByProfile[profileType] ?: return null
        return entriesForProfile.find { !it.allocated }
    }

    /**
     * Allocate by ICCID, but only do so if the profileStatusList exists, and the
     * profile  associated with that ICCID matches the expected profile type
     * (if not null, null will match anything).
     */
    private fun findUnallocatedByIccidAndProfileType(iccid: String, profileType: String?): SmDpSimEntry {
        if (!entriesByIccid.containsKey(iccid)) {
            throw RuntimeException("Attempt to allocate nonexisting profileStatusList $iccid")
        }

        val entry = entriesByIccid[iccid]!!

        if (entry.allocated) {
            throw SmDpPlusException("Attempt to download an already allocated SIM entry")
        }

        if (profileType != null) {
            if (entry.profile != profileType) {
                throw SmDpPlusException("Profile of profileStatusList = $iccid is ${entry.profile}, not $profileType")
            }
        }
        return entry
    }

    /**
     *  Generate a fixed corresponding EID based on ICCID.
     *  XXX Whoot?
     **/
    private fun getEidFromIccid(iccid: String): String? = if (iccid.isNotEmpty())
        "01010101010101010101" + iccid.takeLast(12)
    else
        null

    override fun confirmOrder(eid: String?, iccid: String?, smdsAddress: String?, machingId: String?, confirmationCode: String?, releaseFlag: Boolean): Es2ConfirmOrderResponse {

        if (iccid == null) {
            throw RuntimeException("No ICCD, cannot confirm order")
        }
        if (!entriesByIccid.containsKey(iccid)) {
            throw RuntimeException("Attempt to allocate nonexisting profileStatusList $iccid")
        }
        val entry = entriesByIccid[iccid]!!


        if (smdsAddress != null) {
            entry.smdsAddress = smdsAddress
        }

        if (machingId != null) {
            entry.machingId = confirmationCode
        } else {
            entry.machingId = "0123-ABCD-KGBC-IAMOS-SAD0"  /// XXX This is obviously bogus code!
        }

        entry.released = releaseFlag

        if (confirmationCode != null) {
            entry.confirmationCode = confirmationCode
        }

        val eidReturned = if (eid.isNullOrEmpty())
            getEidFromIccid(iccid)
        else
            eid

        return Es2ConfirmOrderResponse(eS2SuccessResponseHeader(),
                eid = eidReturned!!,
                smdsAddress = entry.smdsAddress,
                matchingId = entry.machingId)
    }

    @Throws(org.ostelco.sim.es2plus.SmDpPlusException::class)
    override fun getProfileStatus(iccidList: List<String>): Es2ProfileStatusResponse {
        log.info("In getProfileStatus with iccidList = $iccidList")

        val result: List<ProfileStatus> = iccidList.map { getProfileStatusForIccid(it) }
                .filterNotNull()
        return Es2ProfileStatusResponse(profileStatusList = result)
    }

    private fun getProfileStatusForIccid(iccid: String): ProfileStatus? {
        val entry = entriesByIccid[iccid]
        return if (entry != null) {
            ProfileStatus(iccid = iccid, state = entry.getState())
        } else {
            null
        }
    }

    override fun cancelOrder(eid: String?, iccid: String?, matchingId: String?, finalProfileStatusIndicator: String?) {
        TODO("not implemented")
    }

    override fun releaseProfile(iccid: String) {
        TODO("not implemented")
    }
}

/**
 * Thrown when an non-recoverable error is encountered byt he sm-dp+ implementation.
 */
class SmDpPlusException(message: String) : Exception(message)


/**
 * Configuration class for SM-DP+ emulator.
 */
data class SmDpPlusAppConfiguration(
        /**
         * Configuring how the Open API representation of the
         * served resources will be presenting itself (owner,
         * license etc.)
         */
        @JsonProperty("es2plusClient")
        val es2plusConfig: EsTwoPlusConfig = EsTwoPlusConfig(),

        /**
         * Configuring how the Open API representation of the
         * served resources will be presenting itself (owner,
         * license etc.)
         */
        val openApi: OpenapiResourceAdderConfig = OpenapiResourceAdderConfig(),

        /**
         * Path to file containing simulated SIM data.
         */
        @JsonProperty("simBatchData")
        val simBatchData: String = "",

        /**
         * The httpClient we use to connect to other services, including
         * ES2+ services
         */
        @JsonProperty("httpClient")
        val httpClientConfiguration: HttpClientConfiguration = HttpClientConfiguration(),

        /**
         * Declaring the mapping between users and certificates, also
         * which roles the users are assigned to.
         */
        @JsonProperty("certAuth")
        val certConfig: CertAuthConfig = CertAuthConfig(),

        /**
         * Declaring which roles we will permit
         */
        @JsonProperty("roles")
        val rolesConfig: RolesConfig = RolesConfig()
) : Configuration()
