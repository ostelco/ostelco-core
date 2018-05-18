package org.ostelco.prime.admin.api


import org.ostelco.prime.getResource
import org.ostelco.prime.model.AdminProduct
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.storage.DataStore
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam

@Path("/offers")
class OfferResource() {

    private var dataStore: DataStore = getResource()

    @GET
    fun getOffers() = dataStore.getOffers().map { it.id }

    @GET
    @Path("/{offer-id}")
    fun getOffer(@PathParam("offer-id") offerId: String) = dataStore.getOffer(offerId)

    @POST
    fun createOffer(offer: Offer) = dataStore.createOffer(toStoredOffer(offer))

    private fun toStoredOffer(offer: Offer): org.ostelco.prime.model.Offer {
        val storedOffer = org.ostelco.prime.model.Offer()
        storedOffer.id = offer.id
        storedOffer.segments = offer.segments.map { dataStore.getSegment(it) }.requireNoNulls()
        storedOffer.products = offer.products.map { dataStore.getProduct(it) }.requireNoNulls()
        return storedOffer
    }
}

@Path("/segments")
class SegmentResource(private val dataStore: DataStore) {

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
        val storedSegment = org.ostelco.prime.model.Segment()
        storedSegment.id = segment.id
        storedSegment.subscribers = segment.subscribers.map { dataStore.getSubscriber(it) }.requireNoNulls()
        return storedSegment
    }
}

@Path("/products")
class ProductResource(private val dataStore: DataStore) {

    @GET
    fun getProducts() = dataStore.getProducts().map { it.id }

    @GET
    @Path("/{product-sku}")
    fun getProducts(@PathParam("product-sku") productSku: String) = dataStore.getProduct(productSku)

    @POST
    fun createProduct(product: AdminProduct) = dataStore.createProduct(product)
}

@Path("/product_classes")
class ProductClassResource(private val dataStore: DataStore) {

    @GET
    fun getProductClasses() = dataStore.getProductClasses().map { it.id }

    @GET
    @Path("/{product-class-id}")
    fun getProductClass(@PathParam("product-class-id") productClassId: String)
            = dataStore.getProductClass(productClassId)

    @POST
    fun createProductClass(productClass: ProductClass) = dataStore.createProductClass(productClass)

    @PUT
    @Path("/{product-class-id}")
    fun updateProductClass(
            @PathParam("product-class-id") productClassId: String,
            productClass: ProductClass): Boolean {
        productClass.id = productClassId
        return dataStore.updateProductClass(productClass)
    }
}