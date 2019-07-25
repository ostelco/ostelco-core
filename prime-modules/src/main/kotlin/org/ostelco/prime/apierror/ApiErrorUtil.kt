package org.ostelco.prime.apierror

import arrow.core.Either
import arrow.core.flatMap
import org.ostelco.prime.jsonmapper.asJson
import javax.ws.rs.core.Response

fun Either<ApiError, Any>.responseBuilder(success: Response.Status = Response.Status.OK,
                                          jsonEncode: Boolean = true): Response.ResponseBuilder =
        this.fold(
                { apiError ->
                    Response.status(apiError.status).entity(asJson(apiError))
                },
                {
                    if (success == Response.Status.NO_CONTENT)
                        Response.status(success).entity(if (jsonEncode) asJson("") else "")
                    else
                        Response.status(success).entity(if (jsonEncode) asJson(it) else it)
                }
        )

fun Either<ApiError, Any>.responseBuilder(payload: String,
                                          success: Response.Status = Response.Status.OK,
                                          jsonEncode: Boolean = true): Response.ResponseBuilder =
        this.map {
            payload
        }.responseBuilder(success, jsonEncode)
