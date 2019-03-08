package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import java.util.*
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/customer")
class CustomerResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomer(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getCustomer(identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun createCustomer(@Auth token: AccessTokenPrincipal?,
                       @NotNull profile: Customer,
                       @QueryParam("referred_by") referredBy: String?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.createCustomer(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                profile = profile.copy(
                        id = UUID.randomUUID().toString(),
                        email = token.name,
                        referralId = UUID.randomUUID().toString()),
                referredBy = referredBy)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.CREATED).entity(asJson(it)) })
                .build()
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateCustomer(@Auth token: AccessTokenPrincipal?,
                       @NotNull profile: Customer): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.updateCustomer(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                profile = profile)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @GET
    @Path("stripe-ephemeral-key")
    @Produces(MediaType.APPLICATION_JSON)
    fun getStripeEphemeralKey(
            @Auth token: AccessTokenPrincipal?,
            @QueryParam("api_version") apiVersion: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getStripeEphemeralKey(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                apiVersion = apiVersion)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { stripeEphemeralKey -> Response.status(Response.Status.OK).entity(stripeEphemeralKey) })
                .build()
    }

    @GET
    @Path("new-ekyc-scanId/{countryCode}")
    @Produces(MediaType.APPLICATION_JSON)
    fun newEKYCScanId(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("countryCode")
            countryCode: String
    ): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.newEKYCScanId(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                countryCode = countryCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { scanInformation -> Response.status(Response.Status.OK).entity(scanInformation) })
                .build()
    }

    @GET
    @Path("scanStatus/{scanId}")
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
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                scanId = scanId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { scanInformation -> Response.status(Response.Status.OK).entity(scanInformation) })
                .build()
    }
}