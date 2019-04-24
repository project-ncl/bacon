package org.jboss.bacon.da.rest.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jboss.bacon.da.rest.model.Artifact;
import org.jboss.bacon.da.rest.model.Contains;
import org.jboss.bacon.da.rest.model.ErrorMessage;
import org.jboss.bacon.da.rest.model.ProductArtifact;
import org.jboss.bacon.da.rest.model.ProductWithGav;
import org.jboss.bacon.da.rest.model.Success;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/listings")
@Api(description = "the listings API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2019-04-24T11:30:42.593645+02:00[Europe/Warsaw]")
public interface ListingsApi {

    @POST
    @Path("/blacklist/gav")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Add an artifact to the blacklist", notes = "", response = Success.class, tags = {"blacklist",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class)
    })
    Response addBlackArtifact(@Valid Artifact artifact);

    @POST
    @Path("/whitelist/product")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Add a product to the whitelist", notes = "", response = Success.class, tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class),
            @ApiResponse(code = 400, message = "Name and version parameters are required", response = ErrorMessage.class)
    })
    Response addProduct(@Valid ProductWithGav product);

    @POST
    @Path("/whitelist/gav")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Add an artifact to the whitelist", notes = "", response = Success.class, tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class),
            @ApiResponse(code = 409, message = "Can't add artifact to whitelist, artifact is blacklisted", response = ErrorMessage.class)
    })
    Response addWhiteArtifact(@Valid ProductArtifact productArtifact);

    @GET
    @Path("/whitelist/artifacts/product")
    @Produces({"application/json"})
    @ApiOperation(value = "Get all artifacts of product from the whitelist", notes = "", response = ProductWithGav.class, responseContainer = "List", tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = ProductWithGav.class, responseContainer = "List")
    })
    Response artifactsOfProduct(@QueryParam("name") String name, @QueryParam("version") String version);

    @PUT
    @Path("/whitelist/product")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Change support status of product in whitelist", notes = "", response = Success.class, tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class),
            @ApiResponse(code = 400, message = "All parameters are required", response = ErrorMessage.class),
            @ApiResponse(code = 404, message = "Product not found", response = ErrorMessage.class)
    })
    Response changeProductStatus(@Valid ProductWithGav product);

    @POST
    @Path("/whitelist/fill/gav")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Fill artifacts from given maven pom gav", notes = "", response = Success.class, tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class)
    })
    Response fillFromGAVBom(@Valid ProductArtifact productArtifact);

    @POST
    @Path("/whitelist/fill/scm")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Fill artifacts from given git pom", notes = "", response = Success.class, tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class)
    })
    Response fillFromGitBom(@Valid ProductArtifact productArtifact);

    @GET
    @Path("/blacklist")
    @Produces({"application/json"})
    @ApiOperation(value = "Get all artifacts in the blacklist", notes = "", response = Artifact.class, responseContainer = "List", tags = {"blacklist",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Artifact.class, responseContainer = "List")
    })
    Response getAllBlackArtifacts();

    @GET
    @Path("/whitelist")
    @Produces({"application/json"})
    @ApiOperation(value = "Get all artifacts in the whitelist", notes = "", response = ProductWithGav.class, responseContainer = "List", tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = ProductWithGav.class, responseContainer = "List")
    })
    List<ProductWithGav> getAllWhiteArtifacts();

    @GET
    @Path("/blacklist/ga")
    @Produces({"application/json"})
    @ApiOperation(value = "Get artifacts in the blacklist with given groupid and artifactid", notes = "", response = Artifact.class, responseContainer = "List", tags = {"blacklist",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Artifact.class, responseContainer = "List")
    })
    Response getBlackArtifacts(@QueryParam("groupid") String groupid, @QueryParam("artifactid") String artifactid);

    @GET
    @Path("/whitelist/product")
    @Produces({"application/json"})
    @ApiOperation(value = "Get product from the whitelist", notes = "", response = ProductWithGav.class, responseContainer = "List", tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = ProductWithGav.class, responseContainer = "List")
    })
    Response getProduct(@QueryParam("id") Long id, @QueryParam("name") String name, @QueryParam("version") String version, @QueryParam("supportStatus") String supportStatus);

    @GET
    @Path("/whitelist/products")
    @Produces({"application/json"})
    @ApiOperation(value = "Get all products from the whitelist", notes = "", response = ProductWithGav.class, responseContainer = "List", tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = ProductWithGav.class, responseContainer = "List")
    })
    Response getProducts();

    @GET
    @Path("/blacklist/gav")
    @Produces({"application/json"})
    @ApiOperation(value = "Check if an artifact is in the blacklist", notes = "", response = Contains.class, tags = {"blacklist",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Contains.class),
            @ApiResponse(code = 400, message = "All parameters are required", response = ErrorMessage.class),
            @ApiResponse(code = 404, message = "Artifact is not in the blacklist", response = Contains.class)
    })
    Response isBlackArtifactPresent(@QueryParam("groupid") String groupid, @QueryParam("artifactid") String artifactid, @QueryParam("version") String version);

    @GET
    @Path("/whitelist/artifacts/gastatus")
    @Produces({"application/json"})
    @ApiOperation(value = "Get all artifacts with specified GA and status from the whitelist", notes = "", response = ProductWithGav.class, responseContainer = "List", tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = ProductWithGav.class, responseContainer = "List")
    })
    Response productsWithArtifactGAAndStatus(@QueryParam("groupid") String groupid, @QueryParam("artifactid") String artifactid, @QueryParam("status") String status);

    @GET
    @Path("/whitelist/artifacts/gav")
    @Produces({"application/json"})
    @ApiOperation(value = "Get all artifacts with specified GAV from the whitelist", notes = "", response = ProductWithGav.class, responseContainer = "List", tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = ProductWithGav.class, responseContainer = "List")
    })
    Response productsWithArtifactGAV(@QueryParam("groupid") String groupid, @QueryParam("artifactid") String artifactid, @QueryParam("version") String version) ;

    @GET
    @Path("/whitelist/artifacts/status")
    @Produces({"application/json"})
    @ApiOperation(value = "Get all artifacts with specified status from the whitelist", notes = "", response = ProductWithGav.class, responseContainer = "List", tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = ProductWithGav.class, responseContainer = "List")
    })
    Response productsWithArtifactStatus(@QueryParam("status") String status) ;

    @DELETE
    @Path("/blacklist/gav")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Remove an artifact from the blacklist", notes = "", response = Success.class, tags = {"blacklist",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class)
    })
    Response removeBlackArtifact(@Valid Artifact artifact) ;

    @DELETE
    @Path("/whitelist/product")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Remove a product from the whitelist", notes = "", response = Success.class, tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class),
            @ApiResponse(code = 404, message = "Product not found", response = ErrorMessage.class)
    })
    Response removeProduct(@Valid ProductWithGav product) ;

    @DELETE
    @Path("/whitelist/gav")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Remove an artifact from the whitelist", notes = "", response = Success.class, tags = {"listings",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class)
    })
    Response removeWhiteArtifact(@Valid Artifact artifact) ;

    @DELETE
    @Path("/whitelist/gavproduct")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiOperation(value = "Remove an artifact from the product", notes = "", response = Success.class, tags = {"listings"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = Success.class)
    })
    Response removeWhiteArtifactFromProduct(@Valid ProductArtifact productArtifact);
}
