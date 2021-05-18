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

import org.jboss.da.lookup.model.MavenLatestRequest;
import org.jboss.da.lookup.model.MavenLatestResult;
import org.jboss.da.lookup.model.MavenLookupRequest;
import org.jboss.da.lookup.model.MavenLookupResult;
import org.jboss.da.lookup.model.NPMLookupRequest;
import org.jboss.da.lookup.model.NPMLookupResult;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.util.Set;

@Path("/lookup")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface LookupApi {

    @POST
    @Path(value = "/maven")
    Set<MavenLookupResult> lookupMaven(MavenLookupRequest request);

    @POST
    @Path(value = "/maven/latest")
    Set<MavenLatestResult> lookupMaven(MavenLatestRequest request);

    @POST
    @Path(value = "/npm")
    Set<NPMLookupResult> lookupNPM(NPMLookupRequest request);
}
