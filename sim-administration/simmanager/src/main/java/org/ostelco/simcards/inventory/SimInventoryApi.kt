package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.instances.either.monad.flatMap
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.StoreError
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration


class SimInventoryApi(private val httpClient: CloseableHttpClient,
                      private val config: SimAdministrationConfiguration,
                      private val dao: SimInventoryDAO) {


    fun getSimProfileStatus(hlrName: String, iccid: String): Either<StoreError, ProfileStatus> =
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        dao.getProfileVendorAdapterById(simEntry.profileVendorId)
                                .flatMap { profileVendorAdapter ->
                                    val config: ProfileVendorConfig? = config.profileVendors.firstOrNull {
                                        it.name == profileVendorAdapter.name
                                    }
                                    if (config != null)
                                        profileVendorAdapter.getProfileStatus(httpClient, config, iccid)
                                    else
                                        NotFoundError("", "")
                                                .left()
                                }
                    }

    fun findSimProfileByIccid(hlrName: String, iccid: String): Either<StoreError, SimEntry> =
            dao.getSimProfileByIccid(iccid)
                    .flatMap { simEntry ->
                        dao.getHlrAdapterById(simEntry.hlrId)
                                .flatMap { hlrAdapter ->
                                    if (hlrName != hlrAdapter.name) {
                                        NotFoundError("sim_entries", "${hlrName} - ${iccid}")
                                                .left()
                                    } else {
                                        simEntry.right()
                                    }
                                }
                    }



}