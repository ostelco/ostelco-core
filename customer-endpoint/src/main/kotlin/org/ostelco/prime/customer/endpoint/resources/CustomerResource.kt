package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.Encoded
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/customer")
class CustomerResource(private val dao: SubscriberDAO) {
    private val logger by getLogger()

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

    private fun decodeEmail(email: String): String {
        // if the email is percent encoded, decode it
        if (email.contains("%")) {
            var toConvert = email
            // + will be decoded to space, we will keep it as +
            if (email.contains("+")) {
                toConvert = email.replace("+", "%2B")
            }
            return URLDecoder.decode(toConvert, StandardCharsets.UTF_8)
        }
        return email
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun createCustomer(@Auth token: AccessTokenPrincipal?,
                       @NotNull @QueryParam("nickname") nickname: String,
                       @NotNull @QueryParam("contactEmail") @Encoded contactEmail: String,
                       @QueryParam("referredBy") referredBy: String?): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        logger.info("Create customer with contactEmail = ${decodeEmail(contactEmail)} encoded = $contactEmail")
        return dao.createCustomer(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                customer = Customer(
                        id = UUID.randomUUID().toString(),
                        nickname = nickname,
                        contactEmail = decodeEmail(contactEmail),
                        analyticsId = UUID.randomUUID().toString(),
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
                       @QueryParam("nickname") nickname: String?,
                       @QueryParam("contactEmail") @Encoded contactEmail: String?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        var decodedEmail = contactEmail
        if (contactEmail != null) {
            decodedEmail = decodeEmail(contactEmail)
        }
        logger.info("Update customer with contactEmail = $decodedEmail")
        return dao.updateCustomer(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                nickname = nickname,
                contactEmail = decodedEmail)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    fun removeCustomer(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.removeCustomer(identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.NO_CONTENT).entity(asJson("")) })
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
}