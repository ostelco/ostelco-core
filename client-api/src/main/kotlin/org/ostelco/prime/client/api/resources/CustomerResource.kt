package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/customer")
class CustomerResource(private val dao: SubscriberDAO) {

    @GET
    @Path("activePseudonyms")
    @Produces(MediaType.APPLICATION_JSON)
    fun getActivePseudonyms(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getActivePseudonymForSubscriber(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider)).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { pseudonym -> Response.status(Response.Status.OK).entity(pseudonym) })
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

    @GET
    @Path("customerState")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscriberState(
            @Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getSubscriberState(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { state -> Response.status(Response.Status.OK).entity(state) })
                .build()
    }
}