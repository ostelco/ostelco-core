package org.ostelco.prime.admin.api


import org.ostelco.prime.module.getResource
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.storage.DataStore
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam

@Path("/offers")
class OfferResource() {

    private val dataStore by lazy { getResource<DataStore>() }

    @GET
    fun getOffers() = dataStore.getOffers().map { it.id }

    @GET
    @Path("/{offer-id}")
    fun getOffer(@PathParam("offer-id") offerId: String) = dataStore.getOffer(offerId)

    @POST
    fun createOffer(offer: Offer) = dataStore.createOffer(toStoredOffer(offer))

    private fun toStoredOffer(offer: Offer): org.ostelco.prime.model.Offer {
        return org.ostelco.prime.model.Offer(
                offer.id,
                offer.segments.map { dataStore.getSegment(it) }.requireNoNulls(),
                offer.products.map { dataStore.getProduct(it) }.requireNoNulls())
    }
}

@Path("/segments")
class SegmentResource {

    private val dataStore by lazy { getResource<DataStore>() }

    @GET
    fun getSegments() = dataStore.getSegments().map { it.id }

    @GET
    @Path("/{segment-id}")
    fun getSegment(@PathParam("segment-id") segmentId: String) = dataStore.getSegment(segmentId)

    @POST
    fun createSegment(segment: Segment) = dataStore.createSegment(toStoredSegment(segment))

    @PUT
    @Path("/{segment-id}")
    fun updateSegment(
            @PathParam("segment-id") segmentId: String,
            segment: Segment): Boolean {
        segment.id = segmentId
        return dataStore.updateSegment(toStoredSegment(segment))
    }

    private fun toStoredSegment(segment: Segment): org.ostelco.prime.model.Segment {
        return org.ostelco.prime.model.Segment(
                segment.id,
                segment.subscribers.map { dataStore.getSubscriber(it) }.requireNoNulls())
    }
}

@Path("/products")
class ProductResource {

    private val dataStore by lazy { getResource<DataStore>() }

    @GET
    fun getProducts() = dataStore.getProducts().map { it.id }

    @GET
    @Path("/{product-sku}")
    fun getProducts(@PathParam("product-sku") productSku: String) = dataStore.getProduct(productSku)

    @POST
    fun createProduct(product: Product) = dataStore.createProduct(product)
}

@Path("/product_classes")
class ProductClassResource {

    private val dataStore by lazy { getResource<DataStore>() }

    @GET
    fun getProductClasses() = dataStore.getProductClasses().map { it.id }

    @GET
    @Path("/{product-class-id}")
    fun getProductClass(@PathParam("product-class-id") productClassId: String) = dataStore.getProductClass(productClassId)

    @POST
    fun createProductClass(productClass: ProductClass) = dataStore.createProductClass(productClass)

    @PUT
    @Path("/{product-class-id}")
    fun updateProductClass(
            @PathParam("product-class-id") productClassId: String,
            productClass: ProductClass): Boolean {
        return dataStore.updateProductClass(
                productClass.copy(id = productClassId))
    }
}