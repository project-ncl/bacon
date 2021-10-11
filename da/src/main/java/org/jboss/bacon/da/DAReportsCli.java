package org.jboss.bacon.da;

import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.da.rest.endpoint.ReportsApi;
import org.jboss.da.model.rest.NPMPackage;
import org.jboss.da.reports.model.api.SCMLocator;
import org.jboss.da.reports.model.request.AlignReportRequest;
import org.jboss.da.reports.model.request.BuiltReportRequest;
import org.jboss.da.reports.model.request.SCMReportRequest;
import org.jboss.da.reports.model.request.VersionsNPMRequest;
import org.jboss.da.reports.model.response.NPMVersionsReport;
import org.jboss.da.reports.model.response.Report;
import org.jboss.da.reports.model.response.striped.AdvancedReport;
import org.jboss.da.reports.model.response.striped.WLStripper;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "reports",
        description = "DA reports endpoint",
        subcommands = {
                DAReportsCli.SCMReport.class,
                DAReportsCli.SCMAdvancedReport.class,
                DAReportsCli.AlignReport.class,
                DAReportsCli.BuiltReport.class,
                DAReportsCli.LookupVersionsNPMReport.class })
@Slf4j
public class DAReportsCli {

    @CommandLine.Command(
            name = "scm",
            description = "Get dependency report for a project specified in a repository URL.")
    public static class SCMReport extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(
                names = "--repositories",
                description = "Additional maven repositories required by the analysed project.")
        private List<String> repositories = new ArrayList<>();

        @CommandLine.Parameters(description = "Scm url of project repository to analyze.")
        private String scm;

        @CommandLine.Parameters(description = "Revision to analyze.")
        private String revision;

        @CommandLine.Parameters(description = "Path to pom.xml file.")
        private String pomPath;

        @Override
        public Integer call() throws Exception {
            SCMLocator locator = SCMLocator.generic(scm, revision, pomPath, repositories);
            SCMReportRequest request = new SCMReportRequest(Collections.emptySet(), Collections.emptySet(), locator);

            ReportsApi reportsApi = DaHelper.createReportsApi();
            try {
                Report result = reportsApi.scmGenerator(request);
                ObjectHelper.print(false, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(
            name = "scm-advanced",
            description = "Get advanced dependency report for a project specified in a repository URL.")
    public static class SCMAdvancedReport extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(
                names = "--repositories",
                description = "Aditional maven repositories required by the analysed project.")
        private List<String> repositories = new ArrayList<>();
        @CommandLine.Parameters(description = "Scm url of project repository to analyze.")
        private String scm;

        @CommandLine.Parameters(description = "Revision to analyze.")
        private String revision;

        @CommandLine.Parameters(description = "Path to pom.xml file.")
        private String pomPath;

        @Override
        public Integer call() throws Exception {
            SCMLocator locator = SCMLocator.generic(scm, revision, pomPath, repositories);
            SCMReportRequest request = new SCMReportRequest(Collections.emptySet(), Collections.emptySet(), locator);

            ReportsApi reportsApi = DaHelper.createReportsApi();
            try {
                AdvancedReport result = WLStripper.strip(reportsApi.advancedScmGenerator(request));
                ObjectHelper.print(false, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(
            name = "align",
            description = "Get alignment report for project specified in a repository URL.")
    public static class AlignReport extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(
                names = "--repositories",
                description = "Aditional maven repositories required by the analysed project.")
        private List<String> repositories = new ArrayList<>();

        @CommandLine.Parameters(description = "Scm url of project repository to analyze.")
        private String scm;

        @CommandLine.Parameters(description = "Revision to analyze.")
        private String revision;

        @CommandLine.Parameters(description = "Path to pom.xml file.")
        private String pomPath;

        @Override
        public Integer call() throws Exception {
            AlignReportRequest request = new AlignReportRequest();
            request.setScmUrl(scm);
            request.setRevision(revision);
            request.setPomPath(pomPath);
            request.setAdditionalRepos(repositories);

            ReportsApi reportsApi = DaHelper.createReportsApi();
            try {
                org.jboss.da.reports.model.response.striped.AlignReport result = WLStripper
                        .strip(reportsApi.alignReport(request));
                ObjectHelper.print(false, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "built", description = "Get built artifacts for project specified in a repository URL.")
    public static class BuiltReport extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(
                names = "--repositories",
                description = "Aditional maven repositories required by the analysed project.")
        private List<String> repositories = new ArrayList<>();

        @CommandLine.Parameters(description = "Scm url of project repository to analyze.")
        private String scm;

        @CommandLine.Parameters(description = "Revision to analyze.")
        private String revision;

        @CommandLine.Parameters(description = "Path to pom.xml file.")
        private String pomPath;

        @Override
        public Integer call() throws Exception {
            BuiltReportRequest request = new BuiltReportRequest();
            request.setScmUrl(scm);
            request.setRevision(revision);
            request.setPomPath(pomPath);
            request.setAdditionalRepos(repositories);

            ReportsApi reportsApi = DaHelper.createReportsApi();
            try {
                Set<org.jboss.da.reports.model.response.BuiltReport> result = reportsApi.builtReport(request);
                ObjectHelper.print(false, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    @CommandLine.Command(
            name = "lookup-versions-npm",
            description = "Lookup and filter versions for the list of provided NPM artifacts.")
    public static class LookupVersionsNPMReport extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(names = "--includeAll", description = "Include artifacts with all quality levels.")
        private Boolean includeAll = true;

        @CommandLine.Option(
                names = "--mode",
                description = "Available search modes: SERVICE_TEMPORARY, PERSISTENT, TEMPORARY, SERVICE, SERVICE_TEMPORARY_PREFER_PERSISTENT, TEMPORARY_PREFER_PERSISTENT")
        private String mode = "PERSISTENT";

        @CommandLine.Parameters(description = "package:version of the packages to lookup")
        private String[] packageVersions;

        @Override
        public Integer call() throws Exception {

            List<NPMPackage> packages = new ArrayList<>();
            for (String npmVersion : packageVersions) {
                packages.add(DaHelper.toNPMPackage(npmVersion));
            }

            VersionsNPMRequest request = VersionsNPMRequest.builder()
                    .packages(packages)
                    .versionFilter(VersionsNPMRequest.VersionFilter.MAJOR_MINOR)
                    .includeAll(includeAll)
                    .mode(mode)
                    .build();

            ReportsApi reportsApi = DaHelper.createReportsApi();
            try {
                List<NPMVersionsReport> result = reportsApi.lookupVersionsNpm(request);
                ObjectHelper.print(false, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }
}
