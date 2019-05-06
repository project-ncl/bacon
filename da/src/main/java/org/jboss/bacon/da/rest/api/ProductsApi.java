package org.jboss.bacon.da.rest.api;

import org.jboss.da.products.model.rest.ProductDiff;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/products")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2019-04-24T11:30:42.593645+02:00[Europe/Warsaw]")
public interface ProductsApi {

    @GET
    @Path("/diff")
    ProductDiff getProduct1(@QueryParam("leftProduct")    Long leftProduct, @QueryParam("rightProduct")    Long rightProduct);
}
