package org.ostelco.prime.ekyc

import org.ostelco.prime.model.MyInfoConfig

interface MyInfoKycService {
    fun getConfig() : MyInfoConfig
    fun getPersonData(authorisationCode: String) : MyInfoData?
}

interface DaveKycService {
    fun validate(id: String?) : Boolean
}

data class MyInfoData(
        val uinFin: String,
        val personData: String?
)