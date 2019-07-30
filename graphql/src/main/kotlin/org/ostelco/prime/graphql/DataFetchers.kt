package org.ostelco.prime.graphql

import arrow.core.Either
import com.fasterxml.jackson.core.type.TypeReference
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

val clientDataSource by lazy { getResource<ClientDataSource>() }

class CreateCustomerDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {

        val contactEmail = env.getArgument<String>("contactEmail")
        val name = env.getArgument<String>("nickname")
        val identity = env.getContext<Identity>()

        return clientDataSource.addCustomer(identity = identity, customer = Customer(contactEmail = contactEmail, nickname = name))
                .map{
                    clientDataSource.getCustomer(identity)
                }
                .map{
                    objectMapper.convertValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
                }
                .fold({
                    throw Exception(it.message)
                }, {
                    it
                })
    }
}

class DeleteCustomerDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {

        val identity = env.getContext<Identity>()
        var ret: Map<String, Any>? = null

        return clientDataSource.getCustomer(identity)
                .map {
                    customer -> clientDataSource.removeCustomer(identity)
                        .map {
                            customer
                        }
                }
                .map {
                    objectMapper.convertValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
                }
                .fold({
                    throw Exception(it.message)
                }, {
                    it
                })
    }
}

class CreateScanDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {

        val identity = env.getContext<Identity>()
        val regionCode = env.getArgument<String>("regionCode")

        return clientDataSource.createNewJumioKycScanId(identity = identity, regionCode = regionCode)
                .map {
                    objectMapper.convertValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
                }
                .fold({
                    throw Exception(it.message)
                }, {
                    it
                })
    }
}

class CreateApplicationTokenFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {
        val identity = env.getContext<Identity>()
        val applicationTokenMap = env.getArgument<Map<String, String>>("appliationToken")

        val applicationToken = ApplicationToken(
                applicationID = applicationTokenMap.get("applicationID")!!,
                token = applicationTokenMap.get("token")!!,
                tokenType = applicationTokenMap.get("tokenType")!!
        )

        if (clientDataSource.addNotificationToken(customerId = identity.id, token = applicationToken)) {
            return applicationTokenMap
        } else {
            throw Exception("Failed to store push token.")
        }
    }
}

class PurchaseProductDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {
        val identity = env.getContext<Identity>()
        val sku = env.getArgument<String>("sku")
        val sourceId = env.getArgument<String>("sourceId")

        return clientDataSource.purchaseProduct(
                identity = identity,
                sku = sku,
                sourceId = sourceId,
                saveCard = false
        ).map{
            clientDataSource.getProduct(identity = identity, sku = sku) // Return purchased product, or return updated bundle
        }.map{
            objectMapper.convertValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
        }.fold({
                throw Exception(it.message)
            }, {
                it
            }
        )
    }
}

class CreateSimProfileDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {
        val identity = env.getContext<Identity>()
        val regionCode = env.getArgument<String>("regionCode")
        val profileType = env.getArgument<String>("profileType")

        return clientDataSource.provisionSimProfile(
                identity = identity,
                regionCode = regionCode,
                profileType = profileType
        ).map{
            objectMapper.convertValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
        }.fold({
            throw Exception(it.message)
        }, {
            it
        })
    }
}

class SendEmailWithActivationQrCodeDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {
        val identity = env.getContext<Identity>()
        val regionCode = env.getArgument<String>("regionCode")
        val iccId = env.getArgument<String>("iccId")

        return clientDataSource.sendEmailWithActivationQrCode(
                identity = identity,
                regionCode = regionCode,
                iccId = iccId
        ).map{
            objectMapper.convertValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
        }.fold({
            throw Exception(it.message)
        }, {
            it
        })
    }
}

class CreateAddressAndPhoneNumberDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {
        val identity = env.getContext<Identity>()
        val address = env.getArgument<String>("address")
        val phoneNumber = env.getArgument<String>("phoneNumber")

        // TODO: Is address specific per region or global unique per user? If it's specific per region we need regionCode as well when we store it.
        return clientDataSource.saveAddressAndPhoneNumber(
                identity = identity,
                address = address,
                phoneNumber = phoneNumber
        ).map{
            mapOf("address" to address, "phoneNumber" to phoneNumber)
        }.fold({
            throw Exception(it.message)
        }, {
            it
        })
    }
}

class ValidateNRICDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {
        val identity = env.getContext<Identity>()
        val nric = env.getArgument<String>("nric")

        return clientDataSource.checkNricFinIdUsingDave(
                identity = identity,
                nricFinId = nric
        ).map{
            mapOf("nric" to nric)
        }.fold({
            throw Exception(it.message)
        }, {
            it
        })
    }
}

class ContextDataFetcher : DataFetcher<Map<String, Any>> {

    override fun get(env: DataFetchingEnvironment): Map<String, Any>? {

        return env.getContext<Identity>()?.let { identity ->
            val map = mutableMapOf<String, Any>()
            if (env.selectionSet.contains("customer/*")) {
                clientDataSource.getCustomer(identity)
                        .map { customer ->
                            map.put("customer", objectMapper.convertValue(customer, object : TypeReference<Map<String, Any>>() {}))
                        }
            }
            if (env.selectionSet.contains("bundles/*")) {
                clientDataSource.getBundles(identity)
                        .map { bundles ->
                            map.put("bundles", bundles.map { bundle ->
                                objectMapper.convertValue<Map<String, Any>>(bundle, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            if (env.selectionSet.contains("regions/*")) {
                val regionCode: String? = env.selectionSet.getField("regions").arguments["regionCode"]?.toString()
                if (regionCode.isNullOrBlank()) {
                    clientDataSource.getAllRegionDetails(identity)
                            .map { regions ->
                                map.put("regions", regions.map { region ->
                                    objectMapper.convertValue<Map<String, Any>>(region, object : TypeReference<Map<String, Any>>() {})
                                })
                            }
                } else {
                    clientDataSource.getRegionDetails(identity, regionCode.toLowerCase())
                            .map { region ->
                                map.put("regions",
                                        listOf(objectMapper.convertValue<Map<String, Any>>(region, object : TypeReference<Map<String, Any>>() {}))
                                )
                            }
                }
            }
            if (env.selectionSet.contains("products/*")) {
                clientDataSource.getProducts(identity)
                        .map { productsMap ->
                            map.put("products", productsMap.values.map { product ->
                                objectMapper.convertValue<Map<String, Any>>(product, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            if (env.selectionSet.contains("purchases/*")) {
                clientDataSource.getPurchaseRecords(identity)
                        .map { purchaseRecords ->
                            map.put("purchases", purchaseRecords.map { purchaseRecord ->
                                objectMapper.convertValue<Map<String, Any>>(purchaseRecord, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            map
        }
    }
}
