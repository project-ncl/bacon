/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.bacon.da.rest.endpoint;

import java.util.Collection;
import java.util.List;

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

import org.jboss.da.listings.model.ProductSupportStatus;
import org.jboss.da.listings.model.rest.ContainsResponse;
import org.jboss.da.listings.model.rest.RestArtifact;
import org.jboss.da.listings.model.rest.RestProduct;
import org.jboss.da.listings.model.rest.RestProductArtifact;
import org.jboss.da.listings.model.rest.RestProductGAV;
import org.jboss.da.listings.model.rest.RestProductInput;
import org.jboss.da.listings.model.rest.SuccessResponse;
import org.jboss.da.listings.model.rest.WLFill;

@Path("/listings")
public interface ListingsApi {

    @POST
    @Path("/blacklist/gav")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse addBlackArtifact(@Valid RestArtifact artifact);

    @GET
    @Path("/blacklist")
    @Produces({ "application/json" })
    Collection<RestArtifact> getAllBlackArtifacts();

    @GET
    @Path("/blacklist/ga")
    @Produces({ "application/json" })
    Collection<RestArtifact> getBlackArtifacts(
            @QueryParam("groupid") String groupid,
            @QueryParam("artifactid") String artifactid);

    @GET
    @Path("/blacklist/gav")
    @Produces({ "application/json" })
    ContainsResponse isBlackArtifactPresent(
            @QueryParam("groupid") String groupid,
            @QueryParam("artifactid") String artifactid,
            @QueryParam("version") String version);

    @DELETE
    @Path("/blacklist/gav")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse removeBlackArtifact(@Valid RestArtifact artifact);

    @Deprecated(forRemoval = true)
    @POST
    @Path("/whitelist/product")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse addProduct(RestProductInput product);

    @Deprecated(forRemoval = true)
    @POST
    @Path("/whitelist/gav")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse addWhiteArtifact(RestProductArtifact productArtifact);

    @Deprecated(forRemoval = true)
    @GET
    @Path("/whitelist/artifacts/product")
    @Produces({ "application/json" })
    List<RestProductGAV> artifactsOfProduct(@QueryParam("name") String name, @QueryParam("version") String version);

    @Deprecated(forRemoval = true)
    @PUT
    @Path("/whitelist/product")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse changeProductStatus(RestProductInput product);

    @Deprecated(forRemoval = true)
    @POST
    @Path("/whitelist/fill/gav")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse fillFromGAVBom(RestProductArtifact productArtifact);

    @Deprecated(forRemoval = true)
    @POST
    @Path("/whitelist/fill/scm")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse fillFromGitBom(@Valid WLFill wlFill);

    @Deprecated(forRemoval = true)
    @GET
    @Path("/whitelist")
    @Produces({ "application/json" })
    Collection<RestProductGAV> getAllWhiteArtifacts();

    @Deprecated(forRemoval = true)
    @GET
    @Path("/whitelist/product")
    Collection<RestProduct> getProduct(
            @QueryParam("id") Long id,
            @QueryParam("name") String name,
            @QueryParam("version") String version,
            @QueryParam("supportStatus") ProductSupportStatus supportStatus);

    @Deprecated(forRemoval = true)
    @GET
    @Path("/whitelist/products")
    @Produces({ "application/json" })
    Collection<RestProduct> getProducts();

    @Deprecated(forRemoval = true)
    @GET
    @Path("/whitelist/artifacts/gastatus")
    @Produces({ "application/json" })
    List<RestProductGAV> productsWithArtifactGAAndStatus(
            @QueryParam("groupid") String groupid,
            @QueryParam("artifactid") String artifactid,
            @QueryParam("status") ProductSupportStatus status);

    @Deprecated(forRemoval = true)
    @GET
    @Path("/whitelist/artifacts/gav")
    @Produces({ "application/json" })
    List<RestProductGAV> productsWithArtifactGAV(
            @QueryParam("groupid") String groupid,
            @QueryParam("artifactid") String artifactid,
            @QueryParam("version") String version);

    @Deprecated(forRemoval = true)
    @GET
    @Path("/whitelist/artifacts/status")
    @Produces({ "application/json" })
    List<RestProductGAV> productsWithArtifactStatus(@QueryParam("status") String status);

    @Deprecated(forRemoval = true)
    @DELETE
    @Path("/whitelist/product")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    Response removeProduct(RestProductInput product);

    @Deprecated(forRemoval = true)
    @DELETE
    @Path("/whitelist/gav")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse removeWhiteArtifact(RestArtifact artifact);

    @Deprecated(forRemoval = true)
    @DELETE
    @Path("/whitelist/gavproduct")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    SuccessResponse removeWhiteArtifactFromProduct(RestProductArtifact productArtifact);
}
