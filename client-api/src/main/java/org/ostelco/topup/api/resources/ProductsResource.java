package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Product;
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
 * Products API.
 *
 */
@AllArgsConstructor
@Path("/products")
public class ProductsResource extends ResourceHelpers {

    @NonNull
    private final SubscriberDAO dao;

    @GET
    @Produces({"application/json"})
    public Response getProducts(@Auth AccessTokenPrincipal token) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, List<Product>> result = dao.getProducts(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(getProductsAsJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(result.left().get()))
                 .build();
    }

    @PUT
    @Path("{product-id}")
    @Produces({"application/json"})
    public Response updateProduct(@Auth AccessTokenPrincipal token,
            @NotNull
            @PathParam("product-id") String productId,
            @DefaultValue("true") @QueryParam("accepted") boolean accepted) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Option<Error> error = accepted
            ? dao.acceptProduct(token.getName(), productId)
            : dao.rejectProduct(token.getName(), productId);

        return error.isEmpty()
            ? Response.status(Response.Status.OK)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(error.get()))
                 .build();
    }
}
