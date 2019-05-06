package org.jboss.bacon.da.rest.api;

import org.jboss.da.reports.model.request.AlignReportRequest;
import org.jboss.da.reports.model.request.BuiltReportRequest;
import org.jboss.da.reports.model.request.GAVRequest;
import org.jboss.da.reports.model.request.LookupGAVsRequest;
import org.jboss.da.reports.model.request.SCMReportRequest;
import org.jboss.da.reports.model.response.AdvancedReport;
import org.jboss.da.reports.model.response.AlignReport;
import org.jboss.da.reports.model.response.BuiltReport;
import org.jboss.da.reports.model.response.LookupReport;
import org.jboss.da.reports.model.response.Report;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

@Path("/reports")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2019-04-24T11:30:42.593645+02:00[Europe/Warsaw]")
public interface ReportsApi {

    @POST
    @Path("/scm-advanced")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    AdvancedReport advancedScmGenerator(SCMReportRequest scMReportRequest);

    @POST
    @Path("/align")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    AlignReport alignReport(AlignReportRequest alignReportRequest);

    @POST
    @Path("/built")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    BuiltReport builtReport(BuiltReportRequest builtReportRequest);

    @POST
    @Path("/gav")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    Report gavGenerator(GAVRequest gavRequest);

    @POST
    @Path("/lookup/gavs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    List<LookupReport> lookupGav(LookupGAVsRequest gavRequest);

    @POST
    @Path("/scm")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    Report scmGenerator(SCMReportRequest scMReportRequest);
}
