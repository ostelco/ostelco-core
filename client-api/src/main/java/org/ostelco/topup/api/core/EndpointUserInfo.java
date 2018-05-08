package org.ostelco.topup.api.core;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;


/**
 * If running behind a Google Cloud Endpoint service the Endpoint service will add some
 * information about the user in a HTTP header named:
 *
 *   X-Endpoint-API-UserInfo
 *
 * This class can be used to 'capture' this information if present.
 *
 * To capture the information, add the following to the resource class:
 *
 *   Valid @HeaderParam("X-Endpoint-API-UserInfo") EndpointUserInfo userInfo
 *
 * Ref.: https://cloud.google.com/endpoints/docs/openapi/authenticating-users
 *       Section: Receiving auth results in your API
 */
public class EndpointUserInfo {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointUserInfo.class);

    private final ObjectMapper MAPPER = new ObjectMapper();

    /* Causes an error if decoding of the base64 encoded json doc fails. */
    @NotNull
    private final JsonNode obj;

    public EndpointUserInfo(final String enc) {
        JsonNode obj = null;
        try {
            obj = MAPPER.readTree(decode(enc));
        } catch (JsonParseException e) {
            LOG.error("Parsing of the provided json doc {} failed: {}", enc, e);
        } catch (IOException e) {
            LOG.error("Unexpected error when parsing the json doc {}: {}", enc, e);
        }
        this.obj = obj;
    }

    public boolean hasIssuer() {
        return has("issuer");
    }

    public boolean hasId() {
        return has("id");
    }

    public boolean hasEmail() {
        return has("email");
    }

    public Optional<String> getIssuer() {
        return get("issuer");
    }

    public Optional<String> getId() {
        return get("id");
    }

    public Optional<String> getEmail() {
        return get("email");
    }

    private boolean has(final String key) {
        return obj != null && obj.has(key);
    }

    private Optional<String> get(final String key) {
        return has(key) ? Optional.of(obj.get(key).textValue()) : Optional.empty();
    }

    private String decode(final String enc) {
        return new String(Base64.getDecoder().decode(enc));
    }

    @Override
    public String toString() {
        return obj.toString();
    }
}
