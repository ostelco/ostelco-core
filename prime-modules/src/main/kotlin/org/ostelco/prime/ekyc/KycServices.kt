package org.ostelco.prime.ekyc

interface MyInfoKycService {
    fun getPersonData(authorisationCode: String) : String
}