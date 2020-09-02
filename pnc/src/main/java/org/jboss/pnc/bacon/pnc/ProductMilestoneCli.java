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
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.bacon.pnc.common.AbstractBuildListCommand;
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

import java.time.Instant;
import java.util.Optional;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.jboss.pnc.bacon.pnc.client.PncClientHelper.parseDateFormat;

@Slf4j
@GroupCommandDefinition(
        name = "product-milestone",
        description = "Product Milestones",
        groupCommands = {
                ProductMilestoneCli.Create.class,
                ProductMilestoneCli.Update.class,
                ProductMilestoneCli.CancelMilestoneClose.class,
                ProductMilestoneCli.Get.class,
                ProductMilestoneCli.PerformedBuilds.class,
                ProductMilestoneCli.MilestoneClose.class })
public class ProductMilestoneCli extends AbstractCommand {

    private static final ClientCreator<ProductMilestoneClient> CREATOR = new ClientCreator<>(
            ProductMilestoneClient::new);
    private static final ClientCreator<ProductVersionClient> VERSION_CREATOR = new ClientCreator<>(
            ProductVersionClient::new);

    @CommandDefinition(name = "create", description = "Create product milestone")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Version of product milestone: Format: <d>.<d>.<d>.<word>")
        private String productMilestoneVersion;
        @Option(required = true, name = "product-version-id", description = "Product Version ID")
        private String productVersionId;
        @Option(name = "starting-date", description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(required = true, name = "end-date", description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;
        @Option(
                shortName = 'o',
                overrideRequired = false,
                hasValue = false,
                description = "use json for output (default to yaml)")
        private boolean jsonOutput = false;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

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
            });
        }

        @Override
        public String exampleText() {

            StringBuilder command = new StringBuilder();
            command.append("$ bacon pnc product-milestone create \\\n")
                    .append("\t--product-version-id 3 \\\n")
                    .append("\t--issue-tracker-url http://example.com \\\n")
                    .append("\t--end-date 2030-12-26 1.2.0.CR1");

            return command.toString();
        }
    }

    @CommandDefinition(name = "update", description = "Update product milestone")
    public class Update extends AbstractCommand {

        @Argument(description = "Product Milestone ID")
        private String productMilestoneId;

        @Option(name = "product-milestone-version", description = "Product Milestone Version")
        private String productMilestoneVersion;
        @Option(name = "starting-date", description = "Starting date, default is today: Format: <yyyy>-<mm>-<dd>")
        private String startDate;
        @Option(name = "end-date", description = "End date: Format: <yyyy>-<mm>-<dd>")
        private String endDate;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

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
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc product-milestone update --issue-tracker-url http://lakaz.org 5";
        }
    }

    @CommandDefinition(name = "close", description = "Close milestone")
    public class MilestoneClose extends AbstractCommand {

        @Argument(required = true, description = "Milestone id")
        private String id;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                try (ProductMilestoneClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                    clientAuthenticated.closeMilestone(id);
                    return 0;
                }
            });
        }
    }

    @CommandDefinition(name = "cancel-milestone-close", description = "Cancel milestone close")
    public class CancelMilestoneClose extends AbstractCommand {

        @Argument(required = true, description = "Milestone id")
        private String id;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                try (ProductMilestoneClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                    clientAuthenticated.cancelMilestoneClose(id);
                    return 0;
                }
            });
        }
    }

    @CommandDefinition(name = "get", description = "Get a product milestone by its id")
    public class Get extends AbstractGetSpecificCommand<ProductMilestone> {

        @Override
        public ProductMilestone getSpecific(String id) throws ClientException {
            try (ProductMilestoneClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @CommandDefinition(name = "list-performed-builds", description = "List performed builds")
    public class PerformedBuilds extends AbstractBuildListCommand {

        @Argument(required = true, description = "Milestone id")
        private String id;

        @Override
        public RemoteCollection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
                throws RemoteResourceException {
            try (ProductMilestoneClient client = CREATOR.newClient()) {
                return client.getBuilds(id, buildsFilter, Optional.ofNullable(sort), Optional.ofNullable(query));
            }
        }
    }

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
}
