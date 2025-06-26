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
package org.jboss.pnc.bacon.pnc;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.jboss.pnc.bacon.pnc.client.PncClientHelper.parseDateFormat;

import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractBuildListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.OperationClient;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.requests.DeliverablesAnalysisRequest;
import org.jboss.pnc.dto.requests.MilestoneCloseRequest;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@Command(
        name = "product-milestone",
        description = "Product Milestones",
        subcommands = {
                ProductMilestoneCli.Create.class,
                ProductMilestoneCli.Update.class,
                ProductMilestoneCli.CancelMilestoneClose.class,
                ProductMilestoneCli.Get.class,
                ProductMilestoneCli.PerformedBuilds.class,
                ProductMilestoneCli.MilestoneClose.class,
                ProductMilestoneCli.AnalyzeDeliverables.class,
                ProductMilestoneCli.ListDeliveredArtifacts.class,
                ProductMilestoneCli.GetDeliverableAnalysisOperation.class })
public class ProductMilestoneCli {

    private static final ClientCreator<ProductMilestoneClient> CREATOR = new ClientCreator<>(
            ProductMilestoneClient::new);
    private static final ClientCreator<ProductVersionClient> VERSION_CREATOR = new ClientCreator<>(
            ProductVersionClient::new);

    private static final ClientCreator<OperationClient> OPERATION_CREATOR = new ClientCreator<>(OperationClient::new);

    /**
     * Product Milestone version format is: <d>.<d>.<d>.<word> The first 2 digits must match the digit for the product
     * version
     *
     * @param productVersionId
     * @param milestoneVersion
     * @return
     */
    public static boolean validateProductMilestoneVersion(String productVersionId, String milestoneVersion)
            throws ClientException {
        try (ProductVersionClient client = VERSION_CREATOR.newClient()) {
            ProductVersion productVersionDTO = client.getSpecific(productVersionId);

            String productVersion = productVersionDTO.getVersion();

            if (!milestoneVersion.startsWith(productVersion)) {
                return false;
            }

            String[] items = milestoneVersion.split("\\.");

            if (items.length != 4) {
                return false;
            }

            return items[2].matches("\\d+");
        }
    }

    @Command(
            name = "create",
            description = "Create product milestone",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc product-milestone create \\%n"
                    + "\t--product-version-id 3 \\%n" + "\t--issue-tracker-url http://example.com \\%n"
                    + "\t--end-date 2030-12-26 1.2.0.CR1")
    public static class Create extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Version of product milestone: Format: <d>.<d>.<d>.<word>")
        private String productMilestoneVersion;
        @Option(required = true, names = "--product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(names = "--starting-date", description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(required = true, names = "--end-date", description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;
        @Option(names = "--set-current", defaultValue = "false", description = "Set created milestone as current")
        private boolean current;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            if (!validateProductMilestoneVersion(productVersionId, productMilestoneVersion)) {
                throw new FatalException("The version does not fit the proper format");
            }

            if (startDate == null) {
                startDate = PncClientHelper.getTodayDayInYYYYMMDDFormat();
            }

            Instant startDateInstant = parseDateFormat(startDate);
            Instant endDateInstant = parseDateFormat(endDate);

            ProductVersionRef productVersionRef = ProductVersionRef.refBuilder().id(productVersionId).build();

            ProductMilestone milestone = ProductMilestone.builder()
                    .version(productMilestoneVersion)
                    .productVersion(productVersionRef)
                    .startingDate(startDateInstant)
                    .plannedEndDate(endDateInstant)
                    .build();

            ProductMilestone createdMilestone = null;
            try (ProductMilestoneClient client = CREATOR.newClientAuthenticated()) {
                createdMilestone = client.createNew(milestone);
            }
            if (current) {
                try (ProductVersionClient versionClient = VERSION_CREATOR.newClientAuthenticated()) {
                    ProductVersion productVersion = versionClient.getSpecific(productVersionId);
                    ProductVersion.Builder updateVersion = productVersion.toBuilder()
                            .currentProductMilestone(createdMilestone);
                    versionClient.update(productVersionId, updateVersion.build());
                }
            }
            ObjectHelper.print(getJsonOutput(), createdMilestone);
            return 0;
        }
    }

    @Command(
            name = "update",
            description = "Update product milestone",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc product-milestone update --issue-tracker-url http://lakaz.org 5")
    public static class Update implements Callable<Integer> {

        @Parameters(description = "Product Milestone ID")
        private String productMilestoneId;

        @Option(names = "--product-milestone-version", description = "Product Milestone Version")
        private String productMilestoneVersion;
        @Option(names = "--starting-date", description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(names = "--end-date", description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;
        @Option(names = "--set-current", defaultValue = "false", description = "Set created milestone as current")
        private boolean current;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (ProductMilestoneClient client = CREATOR.newClient()) {
                ProductMilestone productMilestone = client.getSpecific(productMilestoneId);
                ProductMilestone.Builder updated = productMilestone.toBuilder();

                if (isNotEmpty(productMilestoneVersion)) {
                    if (validateProductMilestoneVersion(
                            productMilestone.getProductVersion().getId(),
                            productMilestoneVersion)) {
                        updated.version(productMilestoneVersion);
                    } else {
                        throw new FatalException(
                                "The version ('{}') does not fit the proper format",
                                productMilestoneVersion);
                    }
                }
                if (isNotEmpty(startDate)) {
                    Instant startDateInstant = parseDateFormat(startDate);
                    updated.startingDate(startDateInstant);
                }
                if (isNotEmpty(endDate)) {
                    Instant endDateInstant = parseDateFormat(endDate);
                    updated.plannedEndDate(endDateInstant);
                }

                try (ProductMilestoneClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                    clientAuthenticated.update(productMilestoneId, updated.build());
                }
                if (current) {
                    try (ProductVersionClient versionClient = VERSION_CREATOR.newClientAuthenticated()) {
                        String productVersionId = productMilestone.getProductVersion().getId();
                        ProductMilestone updatedMilestone = client.getSpecific(productMilestoneId);
                        ProductVersion productVersion = versionClient.getSpecific(productVersionId);
                        ProductVersion.Builder updateVersion = productVersion.toBuilder()
                                .currentProductMilestone(updatedMilestone);
                        versionClient.update(productVersionId, updateVersion.build());
                    }
                }
                return 0;
            }
        }
    }

    @Command(name = "close", description = "Close milestone")
    public static class MilestoneClose implements Callable<Integer> {

        @Parameters(description = "Milestone id")
        private String id;

        @Option(
                names = "--skipBrewPush",
                description = "Skip brew push for milestone close operation",
                defaultValue = "false")
        private boolean skipBrewPush;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (ProductMilestoneClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                clientAuthenticated
                        .closeMilestone(id, MilestoneCloseRequest.builder().skipBrewPush(skipBrewPush).build());
                return 0;
            }
        }
    }

    @Command(name = "cancel-milestone-close", description = "Cancel milestone close")
    public static class CancelMilestoneClose implements Callable<Integer> {

        @Parameters(description = "Milestone id")
        private String id;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (ProductMilestoneClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                clientAuthenticated.cancelMilestoneClose(id);
                return 0;
            }
        }
    }

    @Command(name = "get", description = "Get a product milestone by its id")
    public static class Get extends AbstractGetSpecificCommand<ProductMilestone> {

        @Override
        public ProductMilestone getSpecific(String id) throws ClientException {
            try (ProductMilestoneClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "list-performed-builds", description = "List performed builds")
    public static class PerformedBuilds extends AbstractBuildListCommand {

        @Parameters(description = "Milestone id")
        private String id;

        @Override
        public Collection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (ProductMilestoneClient client = CREATOR.newClient()) {
                return client.getBuilds(id, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }

    @Command(name = "analyze-deliverables", description = "Start analysis of deliverables")
    public static class AnalyzeDeliverables extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Milestone id")
        private String id;

        @Option(
                names = "--deliverables-link",
                required = true,
                description = "Link to deliverables to be analysed, can add multiple links")
        private List<URL> deliverablesLink;

        @Option(
                names = "--scratch",
                description = "Whether to run the analysis as scratch (default: false)",
                defaultValue = "false")
        private Boolean runAsScratchAnalysis;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (ProductMilestoneClient client = CREATOR.newClientAuthenticated()) {
                DeliverablesAnalysisRequest deliverablesAnalysisRequest = DeliverablesAnalysisRequest.builder()
                        .deliverablesUrls(deliverablesLink.stream().map(URL::toString).collect(Collectors.toList()))
                        .runAsScratchAnalysis(runAsScratchAnalysis)
                        .build();
                DeliverableAnalyzerOperation deliverableAnalyzerOperation = client
                        .analyzeDeliverables(id, deliverablesAnalysisRequest);
                ObjectHelper.print(getJsonOutput(), deliverableAnalyzerOperation);
                return 0;
            }
        }
    }

    @Command(name = "list-delivered-artifacts", description = "List artifacts delivered in the specified milestone")
    public static class ListDeliveredArtifacts extends AbstractListCommand<Artifact> {

        @Parameters(description = "Milestone id")
        private String id;

        @Override
        public Collection<Artifact> getAll(String sort, String query) throws RemoteResourceException {
            try (ProductMilestoneClient client = CREATOR.newClient()) {
                return client.getDeliveredArtifacts(id, Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(name = "get-deliverables-analysis", description = "Get a deliverables analysis operation status")
    public static class GetDeliverableAnalysisOperation
            extends AbstractGetSpecificCommand<DeliverableAnalyzerOperation> {

        @Override
        public DeliverableAnalyzerOperation getSpecific(String id) throws ClientException {
            try (OperationClient client = OPERATION_CREATOR.newClient()) {
                return client.getSpecificDeliverableAnalyzer(id);
            }
        }
    }
}
