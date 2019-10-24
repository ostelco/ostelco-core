package org.ostelco.at.common

const val graphqlPostQuery = """{ context { customer { nickname contactEmail } bundles { id balance } regions(regionCode:"no") { region { id name } status kycStatusMap { JUMIO MY_INFO NRIC_FIN ADDRESS } simProfiles { iccId eSimActivationCode status alias } } products { sku price { amount currency } properties { productClass } presentation { label } } } }"""

const val graphqlGetQuery = """{context{customer{nickname,contactEmail}bundles{id,balance}regions(regionCode:"no"){region{id,name}status,kycStatusMap{JUMIO,MY_INFO,NRIC_FIN,ADDRESS}simProfiles{iccId,eSimActivationCode,status,alias}}products{sku,price{amount,currency}properties{productClass}presentation{label}}}}"""