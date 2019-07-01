package org.ostelco.prime.admin.resources


import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Segment
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Deprecated(message = "Assigning MSISDN to Customer via Admin API will be removed in future.")
@Path("/admin/subscriptions")
class SubscriptionsResource {

    private val adminDataSource by lazy { getResource<AdminDataSource>() }

    @POST
    fun createSubscription(
            @QueryParam("email") email: String,
            @QueryParam("regionCode") regionCode: String,
            @QueryParam("iccId") iccId: String,
            @QueryParam("alias") alias: String,
            @QueryParam("msisdn") msisdn: String): Response {

        return adminDataSource.addSubscription(
                identity = Identity(email, "EMAIL", "email"),
                regionCode = regionCode,
                iccId = iccId,
                alias = alias,
                msisdn = msisdn)
                .fold({ Response.status(Response.Status.NOT_FOUND).entity(it.message).build() },
                        { Response.status(Response.Status.CREATED).build() })
    }
}

@Path("/admin/offers")
class OfferResource {

    private val adminDataSource by lazy { getResource<AdminDataSource>() }

//    @GET
//    fun getOffers() = adminDataSource.getOffers()

//    @GET
//    @Path("/{offer-id}")
//    fun getOffer(@PathParam("offer-id") offerId: String) = adminDataSource.getOffer(offerId)

    @POST
    fun createOffer(offer: Offer): Response {
        return adminDataSource.createOffer(offer)
                .fold({ Response.status(Response.Status.FORBIDDEN).entity(it.message).build() },
                        { Response.status(Response.Status.CREATED).build() })
    }

//    private fun toStoredOffer(offer: Offer): org.ostelco.prime.model.Offer {
//        return org.ostelco.prime.model.Offer(
//                offer.id,
//                offer.segments.map { adminDataSource.getSegment(it) }.requireNoNulls(),
//                offer.products.map { dataStore.getProduct(null, it) }.requireNoNulls())
//    }
}

@Path("/admin/segments")
class SegmentResource {

    private val adminDataSource by lazy { getResource<AdminDataSource>() }

//    @GET
//    fun getSegments() = adminDataSource.getSegments().map { it.id }

//    @GET
//    @Path("/{segment-id}")
//    fun getSegment(@PathParam("segment-id") segmentId: String) = adminDataSource.getSegment(segmentId)

    /**
     * Create new [Segment]
     */
    @POST
    fun createSegment(segment: Segment): Response {
        return adminDataSource.createSegment(segment)
                .fold({ Response.status(Response.Status.FORBIDDEN).entity(it.message).build() },
                        { Response.status(Response.Status.CREATED).build() })
    }

    /**
     * Update existing [Segment]. Replace existing subscriber list with new list.
     */
    @PUT
    @Path("/{segment-id}")
    fun updateSegment(
            @PathParam("segment-id") segmentId: String,
            segment: Segment): Response {

        if (segment.id != segmentId) {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .entity("segment id in path and body do not match")
                    .build()
        }

        return adminDataSource.updateSegment(segment)
                .fold({ Response.status(Response.Status.NOT_MODIFIED).entity(it.message).build() },
                        { Response.ok().build() })
    }

    /**
     * Add individual subscriber to a [Segment]
     */
    @POST
    @Path("/{segment-id}/subscriber/{subscriber-id}")
    fun addSubscriberToSegment(segment: Segment): Response {
        TODO("Vihang: Needs implementation")
    }

    /**
     * Add individual subscriber to a [Segment]
     */
    @DELETE
    @Path("/{segment-id}/subscriber/{subscriber-id}")
    fun removeSubscriberFromSegment(segment: Segment): Response {
        TODO("Vihang: Needs implementation")
    }

//    private fun toStoredSegment(segment: Segment): org.ostelco.prime.model.Segment {
//        return org.ostelco.prime.model.Segment(
//                segment.id,
//                segment.subscribers.map { dataStore.getSubscriber(it) }.requireNoNulls())
//    }
}