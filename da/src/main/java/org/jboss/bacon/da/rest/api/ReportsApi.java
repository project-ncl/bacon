package org.jboss.bacon.da.rest.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jboss.bacon.da.rest.model.AdvancedReport;
import org.jboss.bacon.da.rest.model.AlignReport;
import org.jboss.bacon.da.rest.model.AlignReportRequest;
import org.jboss.bacon.da.rest.model.BuiltReport;
import org.jboss.bacon.da.rest.model.BuiltReportRequest;
import org.jboss.bacon.da.rest.model.ErrorMessage;
import org.jboss.bacon.da.rest.model.LookupGAVsRequest;
import org.jboss.bacon.da.rest.model.LookupReport;
import org.jboss.bacon.da.rest.model.Report;
import org.jboss.bacon.da.rest.model.SCMReportRequest;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/reports")
@Api(description = "the reports API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2019-04-24T11:30:42.593645+02:00[Europe/Warsaw]")
public interface ReportsApi {

    @POST
    @Path("/scm-advanced")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get dependency report for a project specified in a repository URL", notes = "", response = AdvancedReport.class, tags={ "reports",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = AdvancedReport.class)
    })
    AdvancedReport advancedScmGenerator(@Valid SCMReportRequest scMReportRequest);

    @POST
    @Path("/align")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get alignment report for project specified in a repository URL.", notes = "", response = AlignReport.class, tags={ "reports",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = AlignReport.class)
    })
    AlignReport alignReport(@Valid AlignReportRequest alignReportRequest);

    @POST
    @Path("/built")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get builded artifacts for project specified in a repository URL.", notes = "", response = BuiltReport.class, tags={ "reports",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = BuiltReport.class)
    })
    BuiltReport builtReport(@Valid BuiltReportRequest builtReportRequest);

    @POST
    @Path("/gav")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get dependency report for a GAV ", notes = "", response = Report.class, tags={ "reports",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = Report.class),
        @ApiResponse(code = 404, message = "Requested GAV was not found in repository", response = ErrorMessage.class),
        @ApiResponse(code = 502, message = "Communication with remote repository failed", response = Void.class)
    })
    Response gavGenerator(@Valid Report report);

    @POST
    @Path("/lookup/gavs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Lookup built versions for the list of provided GAVs", notes = "", response = LookupReport.class, responseContainer = "List", tags={ "reports",  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = LookupReport.class, responseContainer = "List"),
        @ApiResponse(code = 502, message = "Communication with remote repository failed", response = Void.class)
    })
    List<LookupReport> lookupGav(@Valid LookupGAVsRequest lookupGAVsRequest);
    @POST
    @Path("/scm")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get dependency report for a project specified in a repository URL", notes = "", response = Report.class, tags={ "reports" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "successful operation", response = Report.class)
    })
    Response scmGenerator(@Valid SCMReportRequest scMReportRequest);
}
