package org.ostelco.prime.graphql

import graphql.schema.idl.RuntimeWiring

fun buildRuntimeWiring(): RuntimeWiring {
    return RuntimeWiring.newRuntimeWiring()
            .type("QueryType") { typeWiring ->
                typeWiring.dataFetcher("context", ContextDataFetcher())
            }
            .type("Mutation") { typeWiring ->
                typeWiring
                        .dataFetcher("createCustomer", CreateCustomerDataFetcher())
                        .dataFetcher("deleteCustomer", DeleteCustomerDataFetcher())
                        .dataFetcher("createScan", CreateScanDataFetcher())
                        .dataFetcher("createApplicationToken", CreateApplicationTokenFetcher())
                        .dataFetcher("purchaseProduct", PurchaseProductDataFetcher())
                        .dataFetcher("createSimProfile", CreateSimProfileDataFetcher())
                        .dataFetcher("sendEmailWithActivationQrCode", SendEmailWithActivationQrCodeDataFetcher())
                        .dataFetcher("createAddressAndPhoneNumber", CreateAddressAndPhoneNumberDataFetcher())
                        .dataFetcher("validateNRIC", ValidateNRICDataFetcher())

            }
            .build()
}
