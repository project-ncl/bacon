package org.jboss.bacon.da.rest.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jboss.bacon.da.rest.model.ProductDiff;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/products")
@Api(description = "the products API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2019-04-24T11:30:42.593645+02:00[Europe/Warsaw]")
public interface ProductsApi {

    @GET
    @Path("/diff")
    @Produces({ "application/json" })
    @ApiOperation(value = "Get difference of two products", notes = "", response = ProductDiff.class, tags={ "products" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = ProductDiff.class)
    })
    Response getProduct1(@QueryParam("leftProduct")    Long leftProduct,@QueryParam("rightProduct")    Long rightProduct);
}
