package org.ostelco.at.admin

import io.dropwizard.testing.FixtureHelpers.fixture
import org.junit.Test
import org.ostelco.at.jersey.post
import javax.ws.rs.core.Response


class GetSubscriptions {

    @Test
    fun `jersey test - POST import of sample-offer-products-segments`() {

        val theBody = fixture("sample-offer-products-segments.yaml")

        // XXX Preferably, chec, return code on this one.
        post<Unit> {
            path = "/imports"
            body = theBody
            headerParams = mapOf("Content-Type" to listOf("text/vnd.yaml"))
        }
    }
}
