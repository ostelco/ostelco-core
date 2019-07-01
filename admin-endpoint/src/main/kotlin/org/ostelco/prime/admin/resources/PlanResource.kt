package org.ostelco.prime.admin.resources

import org.ostelco.prime.admin.CreatePlanRequest
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * Resource used to handle plans related REST calls.
 */
@Path("/plans")
class PlanResource {

    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Return plan details.
     */
    @GET
    @Path("{planId}")
    @Produces("application/json")
    fun get(@NotNull
            @PathParam("planId") planId: String): Response {
        return storage.getPlan(planId).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to fetch plan",
                            ApiErrorCode.FAILED_TO_FETCH_PLAN,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.OK).entity(asJson(it)) }
        ).build()
    }

    /**
     * Creates a plan.
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    fun create(createPlanRequest: CreatePlanRequest): Response {
        return storage.createPlan(
                plan = createPlanRequest.plan,
                stripeProductName = createPlanRequest.stripeProductName,
                planProduct = createPlanRequest.planProduct).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to store plan",
                            ApiErrorCode.FAILED_TO_STORE_PLAN,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.CREATED).entity(asJson(it)) }
        ).build()
    }

    /**
     * Deletes a plan.
     * Note, will fail if there are subscriptions on the plan.
     */
    @DELETE
    @Path("{planId}")
    @Produces("application/json")
    fun delete(@NotNull
               @PathParam("planId") planId: String): Response {
        return storage.deletePlan(planId).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to remove plan",
                            ApiErrorCode.FAILED_TO_REMOVE_PLAN,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.OK).entity(asJson(it)) }
        ).build()
    }
}