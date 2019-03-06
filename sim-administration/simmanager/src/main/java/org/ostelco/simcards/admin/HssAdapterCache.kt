package org.ostelco.simcards.admin

import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.adapter.HssAdapter
import org.ostelco.simcards.adapter.HssEntry
import org.ostelco.simcards.adapter.Wg2HssAdapter
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.slf4j.LoggerFactory


/**
 * Keep a set of HSS entries that can be used when
 * provisioning SIM profiles in remote HSSes.
 */
class HssAdapterCache(
        val hssConfigs: List<HssConfig>,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient) {

    private val log = LoggerFactory.getLogger(javaClass)


    private val lock = Object()
    lateinit private var hssEntries: Collection<HssEntry>
    private val hssAdaptersByName = mutableMapOf<String, HssAdapter>()
    private val hssAdaptersById = mutableMapOf<Long, HssAdapter>()

    init {
        initialize()
    }

    fun getHssEntries(): Collection<HssEntry> {
        synchronized(lock) {
            return hssEntries
        }
    }

    fun initialize() {
        synchronized(lock) {
            this.hssEntries  = simInventoryDAO.getHssEntries()

            if (this.hssEntries.isNullOrEmpty()) {
                log.error("No HSS entries to be found by the DAO")
                return@synchronized
            }

            hssConfigs.forEach {hssConfig ->
                val adapter = Wg2HssAdapter(httpClient, config = hssConfig, dao = simInventoryDAO)
                hssAdaptersByName.put(hssConfig.name, adapter)
                val entryWithName = hssEntries.find { hssEntry -> hssConfig.name == hssEntry.name }
                if (entryWithName != null) {
                    hssAdaptersById.put(entryWithName!!.id, adapter)
                } else {
                    log.error("Could not find hss entry in database with name '${hssConfig.name}'")
                    return@synchronized
                }
            }
        }
    }

    fun getHssAdapterByName(name: String): HssAdapter? {
        synchronized(lock) {
            return hssAdaptersByName[name]
        }
    }

    fun getHssAdapterById(id: Long): HssAdapter? {
        synchronized(lock) {
            return hssAdaptersById[id]
        }
    }
}