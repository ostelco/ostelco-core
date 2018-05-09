package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.EndpointUserInfo;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Offer;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Offers API.
 *
 */
@AllArgsConstructor
@Path("/offers")
public class OffersResource extends ResourceHelpers {

    @NonNull
    private final SubscriberDAO dao;

    @GET
    @Produces({"application/json"})
    public Response getOffers(@Auth AccessTokenPrincipal token,
            @Valid @HeaderParam("X-Endpoint-API-UserInfo") EndpointUserInfo userInfo) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, List<Offer>> result = dao.getOffers(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(getOffersAsJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(result.left().get()))
                 .build();
    }

    @PUT
    @Path("{offer-id}")
    @Produces({"application/json"})
    public Response updateOffer(@Auth AccessTokenPrincipal token,
            @Valid @HeaderParam("X-Endpoint-API-UserInfo") EndpointUserInfo userInfo,
            @NotNull
            @PathParam("offer-id") String offerId,
            @DefaultValue("true") @QueryParam("accepted") boolean accepted) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Option<Error> error = accepted
            ? dao.acceptOffer(token.getName(), offerId)
            : dao.rejectOffer(token.getName(), offerId);

        return error.isEmpty()
            ? Response.status(Response.Status.OK)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(error.get()))
                 .build();
    }
}
