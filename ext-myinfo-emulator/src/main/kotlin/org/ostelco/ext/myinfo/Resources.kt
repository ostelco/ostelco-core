package org.ostelco.ext.myinfo

import org.ostelco.ext.myinfo.JwtUtils.createAccessToken
import org.ostelco.ext.myinfo.JwtUtils.getClaims
import org.ostelco.prime.getLogger
import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/token")
class TokenResource(private val config: MyInfoEmulatorConfig) {

    private val logger by getLogger()

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun getToken(
            @FormParam("grant_type") grantType: String?,
            @FormParam("code") authorisationCode: String?,
            @FormParam("redirect_uri") redirectUri: String?,
            @FormParam("client_id") clientId: String?,
            @FormParam("client_secret") clientSecret: String?,
            @HeaderParam("Authorization") authHeaderString: String?,
            @Context headers: HttpHeaders,
            body: String): Response {

        logger.debug("Content-Type: ${headers.mediaType}")
        logger.debug("Headers >>>\n${headers.requestHeaders.entries.joinToString("\n")}\n<<< End of Headers")
        logger.debug("Body >>>\n$body\n<<< End of Body")

        return when {

            headers.mediaType != MediaType.APPLICATION_FORM_URLENCODED_TYPE ->
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("""{reason: "Invalid Content-Type - ${headers.mediaType}"}""")
                        .build()

            grantType != "authorization_code" ->
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("""{reason: "Invalid grant_type"}""")
                        .build()

            redirectUri != config.myInfoRedirectUri ->
                Response.status(Response.Status.FORBIDDEN)
                        .entity("""{reason: "Invalid redirect_uri"}""")
                        .build()

            clientId != config.myInfoApiClientId ->
                Response.status(Response.Status.FORBIDDEN)
                        .entity("""{reason: "Invalid client_id"}""")
                        .build()

            clientSecret != config.myInfoApiClientSecret ->
                Response.status(Response.Status.FORBIDDEN)
                        .entity("""{reason: "Invalid client_secret"}""")
                        .build()

            else ->
                Response.status(Response.Status.OK).entity("""
                    {
                        "access_token":"${createAccessToken(config.myInfoServerPrivateKey)}",
                        "scope":"mobileno nationality dob name regadd email sex residentialstatus",
                        "token_type":"Bearer",
                        "expires_in":1799
                    }""".trimIndent())
                        .build()
        }
    }
}

@Path("/person")
class PersonResource(private val config: MyInfoEmulatorConfig) {

    private val logger by getLogger()

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{uinFin}")
    fun getToken(
            @PathParam("uinFin") uinFin: String,
            @QueryParam("client_id") clientId: String,
            @QueryParam("attributes") attributes: String,
            @HeaderParam("Authorization") authHeaderString: String,
            @Context headers: HttpHeaders,
            body: String): Response {

        logger.debug("Content-Type: ${headers.mediaType}")
        logger.debug("Headers >>>\n${headers.requestHeaders.entries.joinToString("\n")}\n<<< End of Headers")
        logger.debug("Body >>>\n$body\n<<< End of Body")

        if (!authHeaderString.contains("Bearer ")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("""{reason: "Missing JWT Access Token"}""")
                    .build()
        }

        val claims = getClaims(authHeaderString.substringAfter("Bearer "), config.myInfoServerPublicKey)

        if (claims.body.subject != uinFin) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("""{reason: "Invalid Subject in Access Token"}""")
                    .build()
        }

        if (authHeaderString.startsWith("Bearer ")) {
            return Response
                    .status(Response.Status.OK)
                    .entity(getPersonData(uinFin = uinFin))
                    .build()
        }

        return Response
                .status(Response.Status.OK)
                .entity(JweCompactUtils.encrypt(config.myInfoClientPublicKey, getPersonData(uinFin = uinFin)))
                .build()
    }

    private fun getPersonData(uinFin: String): String = """
{
  "name": {
    "lastupdated": "2018-03-20",
    "source": "1",
    "classification": "C",
    "value": "TAN XIAO HUI"
  },
  "sex": {
    "lastupdated": "2018-03-20",
    "source": "1",
    "classification": "C",
    "value": "F"
  },
  "nationality": {
    "lastupdated": "2018-03-20",
    "source": "1",
    "classification": "C",
    "value": "SG"
  },
  "dob": {
    "lastupdated": "2018-03-20",
    "source": "1",
    "classification": "C",
    "value": "1970-05-17"
  },
  "email": {
    "lastupdated": "2018-08-23",
    "source": "4",
    "classification": "C",
    "value": "myinfotesting@gmail.com"
  },
  "mobileno": {
    "lastupdated": "2018-08-23",
    "code": "65",
    "source": "4",
    "classification": "C",
    "prefix": "+",
    "nbr": "97399245"
  },
  "regadd": {
    "country": "SG",
    "unit": "128",
    "street": "BEDOK NORTH AVENUE 4",
    "lastupdated": "2018-03-20",
    "block": "102",
    "postal": "460102",
    "source": "1",
    "classification": "C",
    "floor": "09",
    "building": "PEARL GARDEN"
  },
  "uinfin": "$uinFin"
}
    """.trimIndent().replace("\n", "").replace(" ", "")
}