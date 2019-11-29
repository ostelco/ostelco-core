package org.ostelco.simcards.resources

import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.simcards.inventory.SimInventoryApi
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/ostelco/sim-inventory/profilevendor")
class ProfileVendorResource(private val api: SimInventoryApi) {

    @GET
    @Path("{profileVendorName}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfileVendorDetails(@NotEmpty
                                @PathParam("profileVendorName") profileVendorName: String): Response =
            Response.status(Response.Status.OK)
                    .build()
}