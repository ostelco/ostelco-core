package org.ostelco.simcards.smdpplus

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.conscrypt.OpenSSLProvider
import org.ostelco.dropwizardutils.*
import org.ostelco.dropwizardutils.OpenapiResourceAdder.Companion.addOpenapiResourceToJerseyEnv
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter.Companion.addEs2PlusDefaultFiltersAndInterceptors
import org.ostelco.sim.es2plus.SmDpPlusServerResource
import org.ostelco.sim.es2plus.SmDpPlusService
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.security.Security
import javax.validation.Valid
import javax.validation.constraints.NotNull


/**
 * NOTE: This is not a proper SM-DP+ application, it is a test fixture
 * to be used when accpetance-testing the sim administration application.
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
        // TODO: application initialization
    }

    lateinit var httpClient: HttpClient

    lateinit var es2plusClient: ES2PlusClient

    override fun run(config: SmDpPlusAppConfiguration,
                     env: Environment) {

        val jerseyEnvironment = env.jersey()

        addOpenapiResourceToJerseyEnv(jerseyEnvironment, config.openApi)
        addEs2PlusDefaultFiltersAndInterceptors(jerseyEnvironment)

        val simEntriesIterator = SmDpSimEntryIterator(FileInputStream(config.simBatchData))
        val smdpPlusService: SmDpPlusService = SmDpPlusEmulator(simEntriesIterator)

        jerseyEnvironment.register(SmDpPlusServerResource(smDpPlus = smdpPlusService))

        jerseyEnvironment.register(CertificateAuthorizationFilter(
                RBACService(rolesConfig = config.rolesConfig,
                        certConfig = config.certConfig)))

        this.httpClient = HttpClientBuilder(env).using(config.httpClientConfiguration).build(getName())
        this.es2plusClient = ES2PlusClient(
                requesterId = config.es2plusConfig.requesterId,
                client = httpClient)
    }


    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {

            Security.insertProviderAt(OpenSSLProvider(), 1)

            SmDpPlusApplication().run(*args)
        }
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
    val entriesLock = Object()

    val entries: MutableSet<SmDpSimEntry> = mutableSetOf()
    val entriesByIccid = mutableMapOf<String, SmDpSimEntry>()
    val entriesByImsi = mutableMapOf<String, SmDpSimEntry>()
    val entriesByProfile = mutableMapOf<String, MutableSet<SmDpSimEntry>>()

    init {
        incomingEntries.forEach {
            entries.add(it)
            entriesByIccid[it.iccid] = it
            entriesByImsi[it.imsi] = it
            val entriesForProfile: MutableSet<SmDpSimEntry>
            if (!entriesByProfile.containsKey(it.profile)) {
                entriesForProfile = mutableSetOf<SmDpSimEntry>()
                entriesByProfile[it.profile] = entriesForProfile
            } else {
                entriesForProfile = entriesByProfile[it.profile]!!
            }
            entriesForProfile.add(it)
        }

        log.info("Just read ${entries.size} SIM entries.")
    }

    // TODO; What about the reservation flag?
    override fun downloadOrder(eid: String?, iccid: String?, profileType: String?): String {
        synchronized(entriesLock) {
            val entry: SmDpSimEntry? = findMatchingFreeProfile(iccid, profileType)

            if (entry == null) {
                throw SmDpPlusException("Could not find download order matching criteria")
            }

            // If an EID is known, then mark this as the IED associated
            // with the entry.
            if (eid != null) {
                entry.eid = eid
            }

            // Then mark the entry as allocated and return the corresponding ICCID.
            entry.allocated = true

            // Finally return the ICCID uniquely identifying the profile instance.
            return entry.iccid
        }
    }

    /**
     * Find a free profile that either matches both iccid and profile type (if iccid != null),
     * or just profile type (if iccid == null).  Throw runtime exception if parameter
     * errors are discovered, but return null if no matching profile is found.
     */
    private fun findMatchingFreeProfile(iccid: String?, profileType: String?): SmDpSimEntry? {
        if (iccid != null) {
            return allocateByIccid(iccid, profileType)
        } else if (profileType == null) {
            throw RuntimeException("No iccid, no profile type, so don't know how to allocate sim entry")
        } else if (!entriesByProfile.containsKey(profileType)) {
            throw SmDpPlusException("Unknown profile type $profileType")
        } else {
            return allocateByProfile(profileType)
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
     * Allocate by ICCID, but only do so if the iccid exists, and the
     * profile  associated with that ICCID matches the expected profile type
     * (if not null, null will match anything).
     */
    private fun allocateByIccid(iccid: String, profileType: String?): SmDpSimEntry {
        if (!entriesByIccid.containsKey(iccid)) {
            throw RuntimeException("Attempt to allocate nonexisting iccid $iccid")
        }

        val entry = entriesByIccid[iccid]!!

        if (entry.allocated) {
            throw SmDpPlusException("Attempt to download an already allocated SIM entry")
        }

        if (profileType != null) {
            if (!entry.profile.equals(profileType)) {
                throw SmDpPlusException("Profile of iccid = $iccid is ${entry.profile}, not $profileType")
            }
        }
        return entry
    }


    override fun confirmOrder(eid: String, smdsAddress: String?, machingId: String?, confirmationCode: String?) {
        TODO("not implemented")
    }

    override fun cancelOrder(eid: String, iccid: String?, matchingId: String?, finalProfileStatusIndicator: String?) {
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


// XXX Move this to the es2+ protocol code.
class EsTwoPlusConfig {
    @Valid
    @NotNull
    @JsonProperty("requesterId")
    var requesterId: String = ""
}

/**
 * Configuration class for SM-DP+ emulator.
 */
class SmDpPlusAppConfiguration : Configuration() {


    /**
     * Configuring how the Open API representation of the
     * served resources will be presenting itself (owner,
     * license etc.)
     */
    @Valid
    @NotNull
    @JsonProperty("es2plus")
    var es2plusConfig = EsTwoPlusConfig()

    /**
     * Configuring how the Open API representation of the
     * served resources will be presenting itself (owner,
     * license etc.)
     */
    @Valid
    @NotNull
    @JsonProperty("openApi")
    var openApi = OpenapiResourceAdderConfig()

    /**
     * Path to file containing simulated SIM data.
     */
    @Valid
    @NotNull
    @JsonProperty("simBatchData")
    var simBatchData: String = ""

    /**
     * The httpClient we use to connect to other services, including
     * ES2+ services
     */
    @Valid
    @NotNull
    @JsonProperty("httpClient")
    var httpClientConfiguration = HttpClientConfiguration()


    /**
     * Declaring the mapping between users and certificates, also
     * which roles the users are assigned to.
     */
    @Valid
    @JsonProperty("certAuth")
    @NotNull
    var certConfig = CertAuthConfig()

    /**
     * Declaring which roles we will permit
     */
    @Valid
    @JsonProperty("roles")
    @NotNull
    var rolesConfig = RolesConfig()
}


