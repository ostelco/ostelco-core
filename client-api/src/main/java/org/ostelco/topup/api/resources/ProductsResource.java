package org.ostelco.topup.api.resources;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.ostelco.prime.model.Product;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Collection;

/**
 * Products API.
 *
 */
@Path("/products")
public class ProductsResource extends ResourceHelpers {

    private final SubscriberDAO dao;

    public ProductsResource(SubscriberDAO dao) {
        this.dao = dao;
    }

    @GET
    @Produces({"application/json"})
    public Response getProducts(@Auth AccessTokenPrincipal token) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, Collection<Product>> result = dao.getProducts(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(asJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(asJson(result.left().get()))
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
            ? Response.status(Response.Status.CREATED)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(asJson(error.get()))
                 .build();
    }
}
