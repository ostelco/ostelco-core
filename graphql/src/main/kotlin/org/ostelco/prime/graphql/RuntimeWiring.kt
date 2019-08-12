package org.ostelco.prime.graphql

import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

fun buildRuntimeWiring(): RuntimeWiring {
    return RuntimeWiring.newRuntimeWiring()
            .type("Query") { typeWiring -> typeWiring
                    .dataFetcher("customer", CustomerDataFetcher()).dataFetcher("purchases", AllPurchasesDataFetcher())
                    .dataFetcher("products", AllProductsDataFetcher())
                    .dataFetcher("validateNric", ValidateNRICDataFetcher())
                    .dataFetcher("resendEmail", SendEmailWithActivationQrCodeDataFetcher())
                    .dataFetcher("bundles", AllBundlesDataFetcher())
            }
            .type("Customer") { typeWiring ->
                typeWiring.dataFetcher("regions", AllRegionsDataFetcher())
                        .dataFetcher("bundles", AllBundlesDataFetcher())
            }
            .type(TypeRuntimeWiring.newTypeWiring("RegionDetails")
                    .typeResolver { env ->
                        env.schema.getObjectType("RegionDetails")
                    })
            // TODO: Not in use yet, used for generic graphql queries
            .type(TypeRuntimeWiring.newTypeWiring("Node")
                    .typeResolver { env ->
                        val javaObject = env.getObject<Any>()
                        println("******************")
                        println(javaObject)
                        println("******************")
                        env.schema.getObjectType("Customer")
                    }
                    .build()
            )
            .type("Mutation") { typeWiring ->
                typeWiring
                        .dataFetcher("createApplicationToken", CreateApplicationTokenDataFetcher())
                        .dataFetcher("createCustomer", CreateCustomerDataFetcher())
                        .dataFetcher("deleteCustomer", DeleteCustomerDataFetcher())
                        .dataFetcher("createJumioScan", CreateJumioScanDataFetcher())
                        .dataFetcher("createAddress", CreateAddressAndPhoneNumberDataFetcher())
                        .dataFetcher("createSimProfile", CreateSimProfileDataFetcher())
                        .dataFetcher("createPurchase", CreatePurchaseDataFetcher())
            }
            .build()
}
