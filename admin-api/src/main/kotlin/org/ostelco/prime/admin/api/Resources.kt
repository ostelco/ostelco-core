package org.ostelco.prime.admin.api


import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.Segment
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("/admin/subscriptions")
class SubscriptionsResource {

    private val adminDataSource by lazy { getResource<AdminDataSource>() }

    @POST
    fun createSubscription(
            @QueryParam("subscription_id") subscriberId: String,
            @QueryParam("msisdn") msisdn: String): Response {

        return adminDataSource.addSubscription(subscriberId, msisdn)
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

    @POST
    fun createSegment(segment: Segment): Response {
        return adminDataSource.createSegment(segment)
                .fold({ Response.status(Response.Status.FORBIDDEN).entity(it.message).build() },
                        { Response.status(Response.Status.CREATED).build() })
    }

    @PUT
    @Path("/{segment-id}")
    fun updateSegment(
            @PathParam("segment-id") segmentId: String,
            segment: Segment): Response {

        return adminDataSource.updateSegment(segment)
                .fold({ Response.status(Response.Status.NOT_MODIFIED).entity(it.message).build() },
                        { Response.ok().build() })
    }

//    private fun toStoredSegment(segment: Segment): org.ostelco.prime.model.Segment {
//        return org.ostelco.prime.model.Segment(
//                segment.id,
//                segment.subscribers.map { dataStore.getSubscriber(it) }.requireNoNulls())
//    }
}

@Path("/admin/products")
class ProductResource {

    private val adminDataSource by lazy { getResource<AdminDataSource>() }

//    @GET
//    fun getProducts() = adminDataSource.getProducts().map { it.id }

//    @GET
//    @Path("/{product-sku}")
//    fun getProducts(@PathParam("product-sku") productSku: String) = adminDataSource.getProduct(null, productSku)

    @POST
    fun createProduct(product: Product): Response {
        return adminDataSource.createProduct(product)
                .fold({ Response.status(Response.Status.FORBIDDEN).entity(it.message).build() },
                        { Response.ok().build() })
    }
}

@Path("/admin/product_classes")
class ProductClassResource {

    private val adminDataSource by lazy { getResource<AdminDataSource>() }

//    @GET
//    fun getProductClasses() = adminDataSource.getProductClasses().map { it.id }
//
//    @GET
//    @Path("/{product-class-id}")
//    fun getProductClass(@PathParam("product-class-id") productClassId: String) = adminDataSource.getProductClass(productClassId)

    @POST
    fun createProductClass(productClass: ProductClass): Response {
        return adminDataSource.createProductClass(productClass)
                .fold({ Response.status(Response.Status.FORBIDDEN).entity(it.message).build() },
                        { Response.ok().build() })
    }

//    @PUT
//    @Path("/{product-class-id}")
//    fun updateProductClass(
//            @PathParam("product-class-id") productClassId: String,
//            productClass: ProductClass): Boolean {
//        return adminDataSource.updateProductClass(
//                productClass.copy(id = productClassId))
//    }
}