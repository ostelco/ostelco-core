package org.ostelco.prime.ekyc

import org.ostelco.prime.model.MyInfoConfig
import java.time.LocalDate

interface MyInfoKycService {
    fun getConfig() : MyInfoConfig
    fun getPersonData(authorisationCode: String) : MyInfoData?
}

interface DaveKycService {
    fun validate(id: String?) : Boolean
}

data class MyInfoData(
        val uinFin: String,
        val birthDate: LocalDate?,
        val passExpiryDate: LocalDate?,
        val personData: String?
)