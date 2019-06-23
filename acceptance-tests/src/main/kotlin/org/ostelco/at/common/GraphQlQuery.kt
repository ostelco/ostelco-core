package org.ostelco.at.common

const val graphqlPostQuery = "{ context { customer { nickname contactEmail } bundles { id balance } regions { region { id name } status kycStatusMap { JUMIO MY_INFO NRIC_FIN ADDRESS_AND_PHONE_NUMBER } simProfiles { iccId eSimActivationCode status alias } } subscriptions { msisdn } } }"

const val graphqlGetQuery = "{context{customer{nickname,contactEmail}bundles{id,balance}regions{region{id,name}status,kycStatusMap{JUMIO,MY_INFO,NRIC_FIN,ADDRESS_AND_PHONE_NUMBER}simProfiles{iccId,eSimActivationCode,status,alias}}subscriptions{msisdn}}}"