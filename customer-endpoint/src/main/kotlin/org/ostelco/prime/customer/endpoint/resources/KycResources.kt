package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.ekyc.MyInfoKycService
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.MyInfoApiVersion
import org.ostelco.prime.model.MyInfoApiVersion.V2
import org.ostelco.prime.model.MyInfoApiVersion.V3
import org.ostelco.prime.module.getResource
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Generic [KycResource] which has eKYC API common for all the countries.
 *
 * If there are any country specific API, then have a country specific KYC Resource which will inherit common APIs and
 * add country specific APIs.
 *
 * For now we have simple case, which can be handled by inheritance.  But, if it starts to get messy, then replace it
 * with Strategy pattern and use composition instead of inheritance.
 *
 */
open class KycResource(private val regionCode: String, private val dao: SubscriberDAO) {

    @Path("/jumio")
    fun jumioResource(): JumioKycResource {
        return JumioKycResource(regionCode = regionCode, dao = dao)
    }
}

/**
 * [SingaporeKycResource] uses [JumioKycResource] via parent class [KycResource].
 * It has Singapore specific eKYC APIs.
 *
 */
class SingaporeKycResource(private val dao: SubscriberDAO): KycResource(regionCode = "sg", dao = dao) {

    private val logger by getLogger()

    // MyInfo v2
    private val myInfoKycService by lazy { getResource<MyInfoKycService>("v2") }

    @GET
    @Path("/myInfo/{authorisationCode}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomerMyInfoData(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("authorisationCode")
            authorisationCode: String): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getCustomerMyInfoData(
                identity = token.identity,
                version = V2,
                authorisationCode = authorisationCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { personalData -> Response.status(Response.Status.OK).entity(personalData) })
                .build()
    }

    @GET
    @Path("/myInfoConfig")
    @Produces(MediaType.APPLICATION_JSON)
    fun getMyInfoConfig(@Auth token: AccessTokenPrincipal?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return Response.status(Response.Status.OK).entity(myInfoKycService.getConfig()).build()
    }

    // MyInfo v3

    @Path("/myInfo/v3")
    fun myInfoV3Resource(): MyInfoResource {
        return MyInfoResource(dao = dao, version = V3, myInfoKycService = getResource("v3"))
    }

    @GET
    @Path("/dave/{nricFinId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkNricFinId(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("nricFinId")
            nricFinId: String): Response {

        logger.info("checkNricFinId for $nricFinId")

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.checkNricFinIdUsingDave(
                identity = token.identity,
                nricFinId = nricFinId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { personalData -> Response.status(Response.Status.OK).entity(personalData) })
                .build()
    }

    @PUT
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    fun saveProfile(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @QueryParam("address")
            address: String,
            @NotNull
            @QueryParam("phoneNumber")
            phoneNumber: String): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.saveAddressAndPhoneNumber(
                identity = token.identity,
                address = address,
                phoneNumber = phoneNumber)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.NO_CONTENT) })
                .build()
    }
}

class MyInfoResource(
        private val dao: SubscriberDAO,
        private val version: MyInfoApiVersion,
        private val myInfoKycService: MyInfoKycService) {

    @GET
    @Path("/personData/{authorisationCode}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomerMyInfoData(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("authorisationCode")
            authorisationCode: String): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getCustomerMyInfoData(
                identity = token.identity,
                version = version,
                authorisationCode = authorisationCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { personalData -> Response.status(Response.Status.OK).entity(personalData) })
                .build()
    }

    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    fun getMyInfoConfig(@Auth token: AccessTokenPrincipal?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return Response.status(Response.Status.OK).entity(myInfoKycService.getConfig()).build()
    }
}

class JumioKycResource(private val regionCode: String, private val dao: SubscriberDAO) {

    @POST
    @Path("/scans")
    @Produces(MediaType.APPLICATION_JSON)
    fun newEKYCScanId(
            @Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.createNewJumioKycScanId(
                identity = token.identity,
                regionCode = regionCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { scanInformation -> Response.status(Response.Status.CREATED).entity(scanInformation) })
                .build()
    }

    @GET
    @Path("/scans/{scanId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getScanStatus(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("scanId")
            scanId: String
    ): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getScanInformation(
                identity = token.identity,
                scanId = scanId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { scanInformation -> Response.status(Response.Status.OK).entity(scanInformation) })
                .build()
    }
}