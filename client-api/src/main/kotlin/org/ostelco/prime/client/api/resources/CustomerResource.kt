package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import javax.validation.constraints.NotNull
import javax.ws.rs.*
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

        return dao.getActivePseudonymForSubscriber(token.name).fold(
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

        return dao.getStripeEphemeralKey(subscriberId = token.name, apiVersion = apiVersion).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { stripeEphemeralKey -> Response.status(Response.Status.OK).entity(stripeEphemeralKey) })
                .build()
    }

    @GET
    @Path("new-ekyc-scanId")
    @Produces(MediaType.APPLICATION_JSON)
    fun newEKYCScanId(
            @Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.newEKYCScanId(subscriberId = token.name).fold(
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

        return dao.getScanInformation(subscriberId = token.name, scanId = scanId).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { scanInformation -> Response.status(Response.Status.OK).entity(scanInformation) })
                .build()
    }

    @GET
    @Path("subscriberState")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscriberState(
            @Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getSubscriberState(subscriberId = token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { state -> Response.status(Response.Status.OK).entity(state) })
                .build()
    }
}