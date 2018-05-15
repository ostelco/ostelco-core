package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Product;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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

    @POST
    @Path("{sku}")
    @Produces({"application/json"})
    public Response purchaseProduct(@Auth AccessTokenPrincipal token,
            @NotNull
            @PathParam("sku") String sku) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Option<Error> error = dao.purchaseProduct(token.getName(), sku);

        return error.isEmpty()
            ? Response.status(Response.Status.OK)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(error.get()))
                 .build();
    }
}
