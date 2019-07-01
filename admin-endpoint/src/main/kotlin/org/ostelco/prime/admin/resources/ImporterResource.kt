package org.ostelco.prime.admin.resources

import org.ostelco.prime.admin.importer.AddToSegments
import org.ostelco.prime.admin.importer.ChangeSegments
import org.ostelco.prime.admin.importer.CreateOffer
import org.ostelco.prime.admin.importer.CreateSegments
import org.ostelco.prime.admin.importer.ImportProcessor
import org.ostelco.prime.admin.importer.RemoveFromSegments
import org.ostelco.prime.admin.importer.UpdateSegments
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.core.Response


/**
 * Resource used to handle the import related REST calls.
 */
@Path("/import")
class ImporterResource(private val processor: ImportProcessor) {

    private val logger by getLogger()

    /**
     * Create new [Offer].
     * Link to new or existing [Product].
     * Link to new or existing [Segment].
     */
    @POST
    @Path("/offer")
    @Consumes("text/vnd.yaml")
    fun createOffer(createOffer: CreateOffer): Response {
        logger.info("POST for /import/offer")
        return processor.createOffer(createOffer).fold(
                    { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                    { Response.status(Response.Status.CREATED) }
        ).build()
    }

    /**
     * Create new [Segment].
     */
    @POST
    @Path("/segments")
    @Consumes("text/vnd.yaml")
    fun createSegment(createSegments: CreateSegments): Response {
        logger.info("POST for /import/segments")

        return processor.createSegments(createSegments).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.CREATED) }
        ).build()
    }

    /**
     * Update [Segment] - Replace all [Subscriber]s under this [Segment].
     */
    @PUT
    @Path("/segments")
    @Consumes("text/vnd.yaml")
    fun importSegment(updateSegments: UpdateSegments): Response {
        logger.info("PUT for /import/segments")

        return processor.updateSegments(updateSegments).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK) }
        ).build()
    }

    /**
     * Add [Subscriber]s to [Segment]
     */
    @POST
    @Path("/segments/subscribers")
    @Consumes("text/vnd.yaml")
    fun importSegment(addToSegments: AddToSegments): Response {
        logger.info("POST for /import/segments/subscribers")

        return processor.addToSegments(addToSegments).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK) }
        ).build()
    }

    /**
     * Remove [Subscriber]s from [Segment]
     */
    @DELETE
    @Path("/segments/subscribers")
    @Consumes("text/vnd.yaml")
    fun importSegment(removeFromSegments: RemoveFromSegments): Response {
        logger.info("DELETE for /import/segments/subscribers")

        return processor.removeFromSegments(removeFromSegments).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK) }
        ).build()
    }

    /**
     * Move [Subscriber]s from one [Segment] to another.
     */
    @PUT
    @Path("/segments/subscribers")
    @Consumes("text/vnd.yaml")
    fun importSegment(changeSegments: ChangeSegments): Response {
        logger.info("PUT for /import/segments/subscribers")

        return processor.changeSegments(changeSegments).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK) }
        ).build()
    }
}