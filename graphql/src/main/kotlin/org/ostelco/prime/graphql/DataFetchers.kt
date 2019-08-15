package org.ostelco.prime.graphql

import graphql.*
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.execution.DataFetcherResult
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.*
import org.ostelco.prime.paymentprocessor.core.ProductInfo

val clientDataSource by lazy { getResource<ClientDataSource>() }

// TODO: Return types and custom types where existing models doesn't do the job, should probably be moved to separate file
data class CreateCustomerPayload(val customer: Customer)
data class CreateApplicationTokenPayload(val applicationToken: ApplicationToken)
data class CreateJumioScanPayload(val jumioScan: ScanInformation)
data class Address(val address: String, val phoneNumber: String)
data class CreateAddressPayload(val address: Address)
data class CreateSimProfilePayload(val simProfile: SimProfile)
data class CreatePurchasePayload(val purchase: ProductInfo) // TODO: Should be a PurchaseRecord
data class NricInfo(val value: String)
data class ValidateNricPayload(val nric: NricInfo)
data class ResendEmailPayload(val simProfile: SimProfile)

// TODO: Increase code reuse by abstracting code in DataFetchers (they are all very similar). The library graphql-java-tools has already done this abstraction.
data class Customer(
        val id: String,
        val nickname: String,
        val contactEmail: String,
        val analyticsId: String,
        val referralId: String,
        val regions: Collection<RegionDetails>? = null
)

class CreateCustomerDataFetcher : DataFetcher<DataFetcherResult<CreateCustomerPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateCustomerPayload> {

        val result = DataFetcherResult.newResult<CreateCustomerPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val contactEmail = input.get("contactEmail")!!
        val nickname = input.get("nickname")!!
        val customer = Customer(
            contactEmail = contactEmail,
            nickname = nickname
        )

        return clientDataSource.addCustomer(identity = identity, customer = customer)
                .fold({
                    err.message("Failed to create customer.")
                    err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
                    result.error(err.build())
                }, {
                    // TODO: If the below fails, will the customer still be created? (If anything fails, the customer creation should be rolled back.)
                    clientDataSource.getCustomer(identity).map{
                        CreateCustomerPayload(customer = Customer(id = it.id, nickname = it.nickname, contactEmail = it.contactEmail, analyticsId = it.analyticsId, referralId =  it.referralId))
                    }
                    .fold({
                        err.message("Failed to get customer.")
                        err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
                        result.error(err.build())
                    }, {
                        result.data(it)
                    })
                }).build()
    }
}

class CreateJumioScanDataFetcher : DataFetcher<DataFetcherResult<CreateJumioScanPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateJumioScanPayload> {

        var result = DataFetcherResult.newResult<CreateJumioScanPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val regionCode = input.get("regionCode")!!

        return clientDataSource.createNewJumioKycScanId(identity = identity, regionCode = regionCode)
                .map {
                    CreateJumioScanPayload(jumioScan = it)
                }
                .fold({
                    err.message("Failed to create jumio scan ID.")
                    err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
                    result.error(err.build())
                }, {
                    result.data(it)
                }).build()
    }
}


class CustomerDataFetcher : DataFetcher<DataFetcherResult<Customer>>{
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Customer> {

        val result = DataFetcherResult.newResult<Customer>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getCustomer(identity).map{
            Customer(id = it.id, nickname = it.nickname, contactEmail = it.contactEmail, analyticsId = it.analyticsId, referralId = it.referralId)
        }.fold({
            err.message("Failed to get customer.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            result.error(err.build())
        }, {
            result.data(it)
        }).build();
    }
}

class DeleteCustomerDataFetcher : DataFetcher<DataFetcherResult<CreateCustomerPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateCustomerPayload> {

        val result = DataFetcherResult.newResult<CreateCustomerPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getCustomer(identity)
                .map {
                    customer -> clientDataSource.removeCustomer(identity)
                        .map {
                            result.data(CreateCustomerPayload(customer=Customer(id = customer.id, contactEmail = customer.contactEmail, nickname = customer.nickname, analyticsId = customer.analyticsId
                            , referralId = customer.referralId)))
                        }
                }
                .fold({
                    err.message("Failed to delete customer.")
                    err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
                    result.error(err.build())
                }, {
                    result
                }).build()
    }
}

class CreateApplicationTokenDataFetcher : DataFetcher<DataFetcherResult<CreateApplicationTokenPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateApplicationTokenPayload> {

        val result = DataFetcherResult.newResult<CreateApplicationTokenPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val applicationID = input.get("applicationID")!!
        val token = input.get("token")!!
        val tokenType = input.get("tokenType")!!
        val applicationToken = ApplicationToken(
                applicationID = applicationID,
                token = token,
                tokenType = tokenType
        )

        if (clientDataSource.addNotificationToken(customerId = identity.id, token = applicationToken)) {
            val payload = CreateApplicationTokenPayload(applicationToken = applicationToken)
            return result.data(payload).build()
        } else {
            err.message("Failed to store push token.")
            // TODO: Add extension of error id, type and possible internal message to make this error the same format as the other errors.
            return result.error(err.build()).build()
        }
    }
}

class CreateSimProfileDataFetcher : DataFetcher<DataFetcherResult<CreateSimProfilePayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateSimProfilePayload> {

        val result = DataFetcherResult.newResult<CreateSimProfilePayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String,String>>("input")
        val regionCode = input.get("regionCode")!!
        val profileType = input.get("profileType")!!


        return clientDataSource.provisionSimProfile(
                identity = identity,
                regionCode = regionCode,
                profileType = profileType
        ).map{
            CreateSimProfilePayload(simProfile = it)
        }.fold({
            err.message("Failed to create sim profile.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class SendEmailWithActivationQrCodeDataFetcher : DataFetcher<DataFetcherResult<ResendEmailPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<ResendEmailPayload> {

        val result = DataFetcherResult.newResult<ResendEmailPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val regionCode = input.get("regionCode")!!
        val iccId = input.get("iccId")!!

        return clientDataSource.sendEmailWithActivationQrCode(
                identity = identity,
                regionCode = regionCode,
                iccId = iccId
        ).map{
            ResendEmailPayload(simProfile=it)
        }.fold({
            err.message("Failed to resend email.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class CreateAddressAndPhoneNumberDataFetcher : DataFetcher<DataFetcherResult<CreateAddressPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateAddressPayload> {

        val result = DataFetcherResult.newResult<CreateAddressPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val address = input.get("address")!!
        val phoneNumber = input.get("phoneNumber")!!

        // TODO: Is address specific per region or global unique per user? If it's specific per region we need regionCode as well when we store it.
        return clientDataSource.saveAddressAndPhoneNumber(
                identity = identity,
                address = address,
                phoneNumber = phoneNumber
        ).map{
            CreateAddressPayload(address=Address(address = address, phoneNumber = phoneNumber))
        }.fold({
            err.message("Failed to create address.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class ValidateNricDataFetcher : DataFetcher<DataFetcherResult<ValidateNricPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<ValidateNricPayload> {

        val result = DataFetcherResult.newResult<ValidateNricPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String,String>>("input")
        val nric = input.get("nric")!!

        return clientDataSource.checkNricFinIdUsingDave(
                identity = identity,
                nricFinId = nric
        ).map{
            ValidateNricPayload(nric = NricInfo(value = nric))
        }.fold({
            err.message("Failed to validate nric.") // TODO: Does the checkNricFinIdUSingDave return a store error if the value is invalid? If so, we might want to consider separating an actual error from an invalid nric by returning positive response for valid / invalid nric and an error on any other unexpected error.
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class AllRegionsDataFetcher : DataFetcher<DataFetcherResult<Collection<RegionDetails>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<RegionDetails>> {

        val result = DataFetcherResult.newResult<Collection<RegionDetails>>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getAllRegionDetails(identity = identity).map{
            it
        }.fold({
            err.message("Failde to get region details.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            result.error(GraphqlErrorBuilder.newError().message(it.message).build())
        }, {

            val regionCode = env.getArgument<String?>("regionCode")
            if (regionCode != null) {
                val filteredList = it.filter{ it.region.id == regionCode}
                result.data(filteredList)
            } else {
                result.data(it)
            }
        }).build()
    }
}

class AllProductsDataFetcher : DataFetcher<DataFetcherResult<Collection<Product>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<Product>> {

        val response = DataFetcherResult.newResult<Collection<Product>>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getProducts(identity = identity).map{
            it.values
        }.fold({
            err.message("Failed to get products.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

class AllBundlesDataFetcher : DataFetcher<DataFetcherResult<Collection<Bundle>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<Bundle>> {

        val response = DataFetcherResult.newResult<Collection<Bundle>>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getBundles(identity).fold({
            err.message("Failed to get bundles.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

/*
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
            throw GraphQLException(it.message)
        }, {
            it
        }
        )
    }
}
*/

class AllPurchasesDataFetcher : DataFetcher<DataFetcherResult<Collection<PurchaseRecord>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<PurchaseRecord>> {
        val identity = env.getContext<Identity>()
        val response = DataFetcherResult.newResult<Collection<PurchaseRecord>>()
        val err = GraphqlErrorBuilder.newError()
        return clientDataSource.getPurchaseRecords(identity = identity).map{
            it
        }.fold({
            err.message("Failed to get purchases.")
            err.extensions(mapOf("id" to it.id, "type" to it.type, "message" to it.message))
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

class CreatePurchaseDataFetcher : DataFetcher<DataFetcherResult<CreatePurchasePayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreatePurchasePayload> {
        val identity = env.getContext<Identity>()
        val input = env.getArgument<Map<String, String>>("input")
        val sku = input.get("sku")!!
        val sourceId = input.get("sourceId")!!
        val response = DataFetcherResult.newResult<CreatePurchasePayload>()
        val err = GraphqlErrorBuilder.newError()

        return clientDataSource.purchaseProduct(
                identity = identity,
                sku = sku,
                sourceId = sourceId,
                saveCard = false
        ).map{
            CreatePurchasePayload(purchase = it)
        }.fold({
            err.message(it.message)
            // TODO: Payment error does not have id and type? We need type.
            err.extensions(mapOf("message" to it.message))
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

// TODO: To use this we need to throw an error inside the DataFetchers
class CustomDataFetcherExceptionHandler : DataFetcherExceptionHandler {

    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters): DataFetcherExceptionHandlerResult {
        val exception = handlerParameters.exception
        val sourceLocation = handlerParameters.sourceLocation
        val path = handlerParameters.path

        // TODO: Any exception thrown while fetching data using graphql ends up here. Should map out what kind of errors and construct the same type of errors as in the DataFetchers. (We need to add type at least inside extensions)
        val error: GraphQLError = when(exception) {
            // is ValidationException -> ValidationDataFetchingGraphQLError(exception.constraintErrors, path, exception, sourceLocation)
            else -> ExceptionWhileDataFetching(path, exception, sourceLocation)
        }
        logger.warn(error.message, exception)
        return DataFetcherExceptionHandlerResult.newResult().error(error).build()
    }

    private val logger by getLogger()
}

