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
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.Fail;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Slf4j
@GroupCommandDefinition(
        name = "product-milestone",
        description = "Product Milestones",
        groupCommands = {
                ProductMilestoneCli.Create.class,
                ProductMilestoneCli.Update.class,
                ProductMilestoneCli.CancelMilestoneClose.class,
                ProductMilestoneCli.Get.class,
                ProductMilestoneCli.PerformedBuilds.class
        })
public class ProductMilestoneCli extends AbstractCommand {

    private static ProductMilestoneClient clientCache;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static ProductMilestoneClient getClient() {
        if (clientCache == null) {
            clientCache = new ProductMilestoneClient(PncClientHelper.getPncConfiguration());
        }
        return clientCache;
    }

    @CommandDefinition(name = "create", description = "Create product milestone")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Version of product milestone: Format: <d>.<d>.<d>.<word>")
        private String productMilestoneVersion;
        @Option(required = true, name = "product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(required = true, name = "issue-tracker-url", description = "Issue Tracker URL")
        private String issueTrackerUrl;
        @Option(name = "starting-date",
                description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(required = true,
                name = "end-date",
                description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                if (!validateProductMilestoneVersion(productVersionId, productMilestoneVersion)) {
                    Fail.fail("The version does not fit the proper format");
                }

                if (startDate == null) {
                    startDate = sdf.format(new Date());
                }

                Instant startDateInstant = parseDateFormat(startDate);
                Instant endDateInstant = parseDateFormat(endDate);

                ProductVersionRef productVersionRef = ProductVersionRef.refBuilder().id(productVersionId).build();

                ProductMilestone milestone = ProductMilestone.builder()
                        .version(productMilestoneVersion)
                        .productVersion(productVersionRef)
                        .issueTrackerUrl(issueTrackerUrl)
                        .startingDate(startDateInstant)
                        .endDate(endDateInstant)
                        .build();

                System.out.println(getClient().createNew(milestone));
            });
        }
    }

    @CommandDefinition(name = "update", description = "Update product milestone")
    public class Update extends AbstractCommand {

        @Argument(description = "Product Milestone ID")
        private String productMilestoneId;

        @Option(name = "product-milestone-version", description = "Product Milestone Version")
        private String productMilestoneVersion;
        @Option(name = "issue-tracker-url", description = "Issue Tracker URL")
        private String issueTrackerUrl;
        @Option(name = "starting-date", description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(name = "end-date", description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                ProductMilestone productMilestone = getClient().getSpecific(productMilestoneId);

                ProductMilestone.Builder updated = productMilestone.toBuilder();

                ObjectHelper.executeIfNotNull(productMilestoneVersion, () -> {

                    try {
                        if (validateProductMilestoneVersion(productMilestone.getProductVersion().getId(), productMilestoneVersion)) {
                            updated.version(productMilestoneVersion);
                        } else {
                            Fail.fail("The version does not fit the proper format");
                        }
                    } catch (Exception e) {
                        Fail.fail(e.getMessage());
                    }
                });

                ObjectHelper.executeIfNotNull(issueTrackerUrl, () -> updated.issueTrackerUrl(issueTrackerUrl));

                ObjectHelper.executeIfNotNull(startDate, () -> {
                    Instant startDateInstant = parseDateFormat(startDate);
                    updated.startingDate(startDateInstant);
                });

                ObjectHelper.executeIfNotNull(endDate, () -> {
                    Instant endDateInstant = parseDateFormat(endDate);
                    updated.endDate(endDateInstant);
                });

                getClient().update(productMilestoneId, updated.build());
            });
        }
    }

    @CommandDefinition(name = "cancel-milestone-close", description = "Cancel milestone close")
    public class CancelMilestoneClose extends AbstractCommand {

        @Argument(required = true, description = "Milestone id")
        private String id;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> getClient().cancelMilestoneClose(id));
        }
    }

    @CommandDefinition(name = "get", description = "Get product milestone")
    public class Get extends AbstractGetSpecificCommand<ProductMilestone> {

        @Override
        public ProductMilestone getSpecific(String id) throws ClientException {
            return getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "get-performed-builds", description = "Get performed builds")
    public class PerformedBuilds extends AbstractListCommand<Build> {

        @Argument(required = true, description = "Milestone id")
        private String id;

        @Override
        public RemoteCollection<Build> getAll(String sort, String query) throws RemoteResourceException {
            // TODO: figure out what to do with BuildsFilter
            return getClient().getBuilds(id, null, Optional.ofNullable(sort), Optional.of(query));
        }
    }

    /**
     * Product Milestone version format is: <d>.<d>.<d>.<word>
     * The first 2 digits must match the digit for the product version
     * @param productVersionId
     * @param milestoneVersion
     * @return
     */
    public static boolean validateProductMilestoneVersion(String productVersionId, String milestoneVersion) throws ClientException {

        ProductVersionClient productVersionClient = new ProductVersionClient(PncClientHelper.getPncConfiguration());
        ProductVersion productVersionDTO = productVersionClient.getSpecific(productVersionId);

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

    /**
     * Format must be: <yyyy>-<mm>-<dd>
     * @param date
     */
    public static Instant parseDateFormat(String date) {

        try {
            Date ret = sdf.parse(date);
            return ret.toInstant();
        } catch (ParseException e) {
            Fail.fail("Date is not in valid format");
            return null;
        }
    }
}
