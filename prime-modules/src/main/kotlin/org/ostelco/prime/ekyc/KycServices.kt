package org.ostelco.prime.ekyc

import org.ostelco.prime.model.MyInfoConfig

interface MyInfoKycService {
    fun getConfig() : MyInfoConfig
    fun getPersonData(authorisationCode: String) : String
}

interface DaveKycService {
    fun validate(id: String?) : Boolean
}