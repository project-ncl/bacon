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

import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.da.reports.model.request.AlignReportRequest;
import org.jboss.da.reports.model.request.BuiltReportRequest;
import org.jboss.da.reports.model.request.LookupGAVsRequest;
import org.jboss.da.reports.model.request.SCMReportRequest;
import org.jboss.da.reports.model.request.VersionsNPMRequest;
import org.jboss.da.reports.model.response.AdvancedReport;
import org.jboss.da.reports.model.response.AlignReport;
import org.jboss.da.reports.model.response.BuiltReport;
import org.jboss.da.reports.model.response.LookupReport;
import org.jboss.da.reports.model.response.NPMVersionsReport;
import org.jboss.da.reports.model.response.Report;

@Path("/reports")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface ReportsApi {

    @POST
    @Path("/scm")
    Report scmGenerator(SCMReportRequest scmReportRequest);

    @POST
    @Path("/scm-advanced")
    AdvancedReport advancedScmGenerator(SCMReportRequest scmReportRequest);

    @POST
    @Path("/align")
    AlignReport alignReport(AlignReportRequest alignReportRequest);

    @POST
    @Path("/built")
    Set<BuiltReport> builtReport(BuiltReportRequest builtReportRequest);

    @Deprecated() //use /lookup/maven endpoint instead
    @POST
    @Path("/lookup/gavs")
    List<LookupReport> lookupGav(LookupGAVsRequest gavRequest);

    @POST
    @Path("/versions/npm")
    List<NPMVersionsReport> lookupVersionsNpm(VersionsNPMRequest request);

}
