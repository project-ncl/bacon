/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pnc;

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
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductReleaseClient;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductRelease;
import org.jboss.pnc.enums.SupportLevel;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@GroupCommandDefinition(
        name = "product-release",
        description = "Product Release",
        groupCommands = {
                ProductReleaseCli.Create.class,
                ProductReleaseCli.Update.class,
                ProductReleaseCli.Get.class,
                ProductReleaseCli.ListSupportLevel.class })
public class ProductReleaseCli extends AbstractCommand {

    private static final ClientCreator<ProductReleaseClient> CREATOR = new ClientCreator<>(ProductReleaseClient::new);
    private static final ClientCreator<ProductMilestoneClient> MILESTONE_CREATOR = new ClientCreator<>(
            ProductMilestoneClient::new);

    @CommandDefinition(name = "create", description = "Create a product release")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Product Release Version. Format: <d>.<d>.<d>.<word>")
        private String productReleaseVersion;

        @Option(name = "release-date", description = "Release date, default is today. Format: <yyyy>-<mm>-<dd>")
        private String releaseDate;
        @Option(
                required = true,
                name = "milestone-id",
                description = "Product Milestone ID from which release is based")
        private String productMilestoneId;
        @Option(
                required = true,
                name = "support-level",
                description = "Support level: potential values: UNRELEASED, EARLYACCESS, SUPPORTED, EXTENDED_SUPPORT, EOL")
        private String supportLevel;
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

                if (releaseDate == null) {
                    releaseDate = PncClientHelper.getTodayDayInYYYYMMDDFormat();
                }

                if (!validateReleaseVersion(productMilestoneId, productReleaseVersion)) {
                    throw new FatalException(
                            "Product Release version ('{}') and milestone ('{}') is not valid!",
                            productReleaseVersion,
                            productMilestoneId);
                }

                // we have to specify the product version, otherwise PNC is not happy. Why though???
                try (ProductMilestoneClient client = MILESTONE_CREATOR.newClient()) {
                    ProductMilestone productMilestone = client.getSpecific(productMilestoneId);

                    ProductRelease productRelease = ProductRelease.builder()
                            .version(productReleaseVersion)
                            .releaseDate(PncClientHelper.parseDateFormat(releaseDate))
                            .productMilestone(productMilestone)
                            .productVersion(productMilestone.getProductVersion())
                            .supportLevel(SupportLevel.valueOf(supportLevel))
                            .build();

                    try (ProductReleaseClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                        ObjectHelper.print(jsonOutput, clientAuthenticated.createNew(productRelease));
                        return 0;
                    }
                }
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc product-release create --milestone-id 32 --support-level EOL 2.1.0.GA";
        }
    }

    @CommandDefinition(name = "update", description = "Update product release")
    public class Update extends AbstractCommand {

        @Argument(required = true, description = "Product Release ID")
        private String productReleaseId;

        @Option(name = "product-release-version", description = "Product Release Version. Format: <d>.<d>.<d>.<word>")
        private String productReleaseVersion;
        @Option(name = "release-date", description = "Release date, default is today. Format: <yyyy>-<mm>-<dd>")
        private String releaseDate;
        @Option(
                name = "support-level",
                description = "Support level: potential values: UNRELEASED, EARLYACCESS, SUPPORTED, EXTENDED_SUPPORT, EOL")
        private String supportLevel;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                try (ProductReleaseClient client = CREATOR.newClient()) {
                    ProductRelease productRelease = client.getSpecific(productReleaseId);
                    ProductRelease.Builder updated = productRelease.toBuilder();

                    if (isNotEmpty(productReleaseVersion)) {
                        if (validateReleaseVersion(
                                productRelease.getProductMilestone().getId(),
                                productReleaseVersion)) {
                            updated.version(productReleaseVersion);
                        } else {
                            throw new FatalException(
                                    "Product Release Version ('{}') is not valid!",
                                    productReleaseVersion);
                        }
                    }
                    if (isNotEmpty(releaseDate)) {
                        updated.releaseDate(PncClientHelper.parseDateFormat(releaseDate));
                    }
                    if (isNotEmpty(supportLevel)) {
                        updated.supportLevel(SupportLevel.valueOf(supportLevel));
                    }

                    try (ProductReleaseClient clientAuthenticated = CREATOR.newClientAuthenticated()) {
                        clientAuthenticated.update(productReleaseId, updated.build());
                        return 0;
                    }
                }
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc product-release update --support-level UNRELEASED 14";
        }
    }

    @CommandDefinition(name = "get", description = "Get a product release by its id")
    public class Get extends AbstractGetSpecificCommand<ProductRelease> {

        @Override
        public ProductRelease getSpecific(String id) throws ClientException {
            try (ProductReleaseClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @CommandDefinition(name = "list-support-levels", description = "List supported levels")
    public class ListSupportLevel extends AbstractCommand {

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
                try (ProductReleaseClient client = CREATOR.newClient()) {
                    ObjectHelper.print(jsonOutput, client.getSupportLevels());
                    return 0;
                }
            });
        }
    }

    static boolean validateReleaseVersion(String productMilestoneId, String productVersion) throws ClientException {
        try (ProductMilestoneClient client = MILESTONE_CREATOR.newClient()) {
            ProductMilestone productMilestone = client.getSpecific(productMilestoneId);

            return ProductMilestoneCli
                    .validateProductMilestoneVersion(productMilestone.getProductVersion().getId(), productVersion);
        }
    }
}
