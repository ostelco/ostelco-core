package org.ostelco.prime.admin.api


import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.Segment
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataStore
import org.ostelco.prime.storage.legacy.Storage
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam

@Path("/offers")
class OfferResource() {

    private val dataStore by lazy { getResource<Storage>() }
    private val adminDataStore by lazy { getResource<AdminDataStore>() }

//    @GET
//    fun getOffers() = adminDataStore.getOffers()

//    @GET
//    @Path("/{offer-id}")
//    fun getOffer(@PathParam("offer-id") offerId: String) = adminDataStore.getOffer(offerId)

    @POST
    fun createOffer(offer: Offer) = adminDataStore.createOffer(offer)

//    private fun toStoredOffer(offer: Offer): org.ostelco.prime.model.Offer {
//        return org.ostelco.prime.model.Offer(
//                offer.id,
//                offer.segments.map { adminDataStore.getSegment(it) }.requireNoNulls(),
//                offer.products.map { dataStore.getProduct(null, it) }.requireNoNulls())
//    }
}

@Path("/segments")
class SegmentResource {

    private val dataStore by lazy { getResource<Storage>() }
    private val adminDataStore by lazy { getResource<AdminDataStore>() }

//    @GET
//    fun getSegments() = adminDataStore.getSegments().map { it.id }

//    @GET
//    @Path("/{segment-id}")
//    fun getSegment(@PathParam("segment-id") segmentId: String) = adminDataStore.getSegment(segmentId)

    @POST
    fun createSegment(segment: Segment) = adminDataStore.createSegment(segment)

    @PUT
    @Path("/{segment-id}")
    fun updateSegment(
            @PathParam("segment-id") segmentId: String,
            segment: Segment) {
        segment.id = segmentId
        adminDataStore.updateSegment(segment)
    }

//    private fun toStoredSegment(segment: Segment): org.ostelco.prime.model.Segment {
//        return org.ostelco.prime.model.Segment(
//                segment.id,
//                segment.subscribers.map { dataStore.getSubscriber(it) }.requireNoNulls())
//    }
}

@Path("/products")
class ProductResource {

    private val dataStore by lazy { getResource<Storage>() }
    private val adminDataStore by lazy { getResource<AdminDataStore>() }

//    @GET
//    fun getProducts() = adminDataStore.getProducts().map { it.id }

    @GET
    @Path("/{product-sku}")
    fun getProducts(@PathParam("product-sku") productSku: String) = dataStore.getProduct(null, productSku)

    @POST
    fun createProduct(product: Product) = adminDataStore.createProduct(product)
}

@Path("/product_classes")
class ProductClassResource {

    private val adminDataStore by lazy { getResource<AdminDataStore>() }

//    @GET
//    fun getProductClasses() = adminDataStore.getProductClasses().map { it.id }
//
//    @GET
//    @Path("/{product-class-id}")
//    fun getProductClass(@PathParam("product-class-id") productClassId: String) = adminDataStore.getProductClass(productClassId)

    @POST
    fun createProductClass(productClass: ProductClass) = adminDataStore.createProductClass(productClass)

//    @PUT
//    @Path("/{product-class-id}")
//    fun updateProductClass(
//            @PathParam("product-class-id") productClassId: String,
//            productClass: ProductClass): Boolean {
//        return adminDataStore.updateProductClass(
//                productClass.copy(id = productClassId))
//    }
}