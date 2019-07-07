package org.ostelco.prime.auth

import com.fasterxml.jackson.core.type.TypeReference
import io.jsonwebtoken.Jwts
import org.ostelco.prime.auth.apple.GrantType
import org.ostelco.prime.auth.apple.TokenResponse
import org.ostelco.prime.jsonmapper.objectMapper
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.test.assertEquals

@Path("/auth")
class AppleIdAuthServiceEmulator {

    @POST
    @Path("/token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun authorize(
            @FormParam("client_id") clientId: String,
            @FormParam("client_secret") clientSecret: String,
            @FormParam("code") authCode: String,
            @FormParam("grant_type") grantType: String): Response {

        assertEquals("CLIENT_ID", clientId)
        assertEquals("AUTH_CODE", authCode)
        assertEquals(GrantType.authorization_code.name, grantType)

        val parts = clientSecret.split('.')
        assertEquals(3, parts.size)
        assertEquals("""{"kid":"KEY_ID","alg":"ES256"}""", String(Base64.getDecoder().decode(parts[0])))
        val map: Map<String, String> = objectMapper.readValue(
                String(Base64.getDecoder().decode(parts[1])),
                object : TypeReference<LinkedHashMap<String, String>>() {}
        )
        assertEquals("TEAM_ID", map["iss"])
        assertEquals("CLIENT_ID", map["sub"])
        return Response.ok(TokenResponse(
                access_token = "ACCESS_TOKEN",
                expires_in = Instant.now().plus(1, ChronoUnit.HOURS).epochSecond,
                id_token = Jwts.builder().setSubject("APPLE_ID").compact(),
                refresh_token = "REFRESH_TOKEN",
                token_type = "bearer"
        )).build()
    }
}