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

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractBuildListCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.jboss.pnc.bacon.pnc.client.PncClientHelper.parseDateFormat;

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
                ProductMilestoneCli.MilestoneClose.class })
public class ProductMilestoneCli {

    private static final ClientCreator<ProductMilestoneClient> CREATOR = new ClientCreator<>(
            ProductMilestoneClient::new);
    private static final ClientCreator<ProductVersionClient> VERSION_CREATOR = new ClientCreator<>(
            ProductVersionClient::new);

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

    @Command(name = "create", description = "Create product milestone")
    public static class Create implements Callable<Integer> {

        @Parameters(description = "Version of product milestone: Format: <d>.<d>.<d>.<word>")
        private String productMilestoneVersion;
        @Option(required = true, names = "--product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(names = "--starting-date", description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(required = true, names = "--end-date", description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;
        @Option(names = "-o", description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

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

            try (ProductMilestoneClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(jsonOutput, client.createNew(milestone));
                return 0;
            }
        }

        // TODO: @Override
        public String exampleText() {

            StringBuilder command = new StringBuilder();
            command.append("$ bacon pnc product-milestone create \\\n")
                    .append("\t--product-version-id 3 \\\n")
                    .append("\t--issue-tracker-url http://example.com \\\n")
                    .append("\t--end-date 2030-12-26 1.2.0.CR1");

            return command.toString();
        }
    }

    @Command(name = "update", description = "Update product milestone")
    public static class Update implements Callable<Integer> {

        @Parameters(description = "Product Milestone ID")
        private String productMilestoneId;

        @Option(names = "--product-milestone-version", description = "Product Milestone Version")
        private String productMilestoneVersion;
        @Option(names = "--starting-date", description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(names = "--end-date", description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;

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
                    return 0;
                }
            }
        }

        // TODO: @Override
        public String exampleText() {
            return "$ bacon pnc product-milestone update --issue-tracker-url http://lakaz.org 5";
        }
    }

    @Command(name = "close", description = "Close milestone")
    public static class MilestoneClose implements Callable<Integer> {

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
                clientAuthenticated.closeMilestone(id);
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
        public RemoteCollection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (ProductMilestoneClient client = CREATOR.newClient()) {
                return client.getBuilds(id, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }
}
