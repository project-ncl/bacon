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
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductReleaseClient;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductRelease;
import org.jboss.pnc.enums.SupportLevel;

@GroupCommandDefinition(
        name = "product-release",
        description = "Product Release",
        groupCommands = {
                ProductReleaseCli.Create.class,
                ProductReleaseCli.Update.class,
                ProductReleaseCli.Get.class,
                ProductReleaseCli.ListSupportLevel.class
        })
public class ProductReleaseCli extends AbstractCommand {

    private static ProductReleaseClient clientCache;

    private static ProductReleaseClient getClient() {
        if (clientCache == null) {
           clientCache = new ProductReleaseClient(PncClientHelper.getPncConfiguration());
        }
        return clientCache;
    }

    @CommandDefinition(name = "create", description = "Create a product release")
    public class Create extends AbstractCommand {

        @Argument(required = true, description = "Product Release Version. Format: <d>.<d>.<d>.<word>")
        private String productReleaseVersion;

        @Option(name = "release-date", description = "Release date, default is today. Format: <yyyy>-<mm>-<dd>")
        private String releaseDate;
        @Option(required = true, name = "milestone-id", description = "Product Milestone ID from which release is based")
        private String productMilestoneId;
        @Option(required = true, name = "support-level",
                description = "Support level: potential values: UNRELEASED, EARLYACCESS, SUPPORTED, EXTENDED_SUPPORT, EOL")
        private String supportLevel;
        @Option(name = "download-url", description = "Internal or public location to download the product distribution artifacts")
        private String downloadUrl;
        @Option(name = "issue-tracker-url", description = "Link to issues fixed in this release")
        private String issueTrackerUrl;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                if (releaseDate == null) {
                    releaseDate = PncClientHelper.getTodayDayInYYYYMMDDFormat();
                }

                if (!validateReleaseVersion(productMilestoneId, productReleaseVersion)) {
                    Fail.fail("Product Release version is not valid!");
                }

                // we have to specify the product version, otherwise PNC is not happy. Why though???
                ProductMilestoneClient productMilestoneClient = new ProductMilestoneClient(PncClientHelper.getPncConfiguration());
                ProductMilestone productMilestone = productMilestoneClient.getSpecific(productMilestoneId);

                ProductRelease productRelease = ProductRelease.builder()
                        .version(productReleaseVersion)
                        .releaseDate(PncClientHelper.parseDateFormat(releaseDate))
                        .productMilestone(productMilestone)
                        .productVersion(productMilestone.getProductVersion())
                        .supportLevel(SupportLevel.valueOf(supportLevel))
                        .downloadUrl(downloadUrl)
                        .issueTrackerUrl(issueTrackerUrl)
                        .build();

                System.out.println(getClient().createNew(productRelease));
            });
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
        @Option(name = "support-level",
                description = "Support level: potential values: UNRELEASED, EARLYACCESS, SUPPORTED, EXTENDED_SUPPORT, EOL")
        private String supportLevel;
        @Option(name = "download-url", description = "Internal or public location to download the product distribution artifacts")
        private String downloadUrl;
        @Option(required = true, name = "issue-tracker-url", description = "Link to issues fixed in this release")
        private String issueTrackerUrl;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                ProductRelease productRelease = getClient().getSpecific(productReleaseId);
                ProductRelease.Builder updated = productRelease.toBuilder();

                ObjectHelper.executeIfNotNull(productReleaseVersion, () -> {

                    try {
                        if (validateReleaseVersion(productRelease.getProductMilestone().getId(), productReleaseVersion)) {
                            updated.version(productReleaseVersion);
                        } else {
                            Fail.fail("Product Release Version '" + productReleaseVersion + "' is not valid!");
                        }
                    } catch (ClientException e) {
                        Fail.fail("Error: " + e.getMessage());
                    }
                });

                ObjectHelper.executeIfNotNull(releaseDate, () -> updated.releaseDate(PncClientHelper.parseDateFormat(releaseDate)));
                ObjectHelper.executeIfNotNull(supportLevel, () -> updated.supportLevel(SupportLevel.valueOf(supportLevel)));
                ObjectHelper.executeIfNotNull(downloadUrl, () -> updated.downloadUrl(downloadUrl));
                ObjectHelper.executeIfNotNull(issueTrackerUrl, () -> updated.issueTrackerUrl(issueTrackerUrl));

                getClient().update(productReleaseId, updated.build());
            });
        }
    }

    @CommandDefinition(name = "get", description = "Get product release")
    public class Get extends AbstractGetSpecificCommand<ProductRelease> {

        @Override
        public ProductRelease getSpecific(String id) throws ClientException {
            return getClient().getSpecific(id);
        }
    }

    @CommandDefinition(name = "list-support-levels", description = "List supported levels")
    public class ListSupportLevel extends AbstractCommand {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {
                // TODO: YAML or JSON format?
                for (SupportLevel supportLevel : getClient().getAllSupportLevel()) {
                    System.out.println(supportLevel);
                }
            });
        }
    }

    private static boolean validateReleaseVersion(String productMilestoneId, String productVersion) throws ClientException {
        ProductMilestoneClient productMilestoneClient = new ProductMilestoneClient(PncClientHelper.getPncConfiguration());
        ProductMilestone productMilestone = productMilestoneClient.getSpecific(productMilestoneId);

        return ProductMilestoneCli.validateProductMilestoneVersion(productMilestone.getProductVersion().getId(), productVersion);
    }
}
