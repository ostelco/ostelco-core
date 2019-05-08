package org.ostelco.prime.ekyc

interface MyInfoKycService {
    fun getPersonData(authorisationCode: String) : String
}

interface DaveKycService {
    fun validate(id: String?) : Boolean
}