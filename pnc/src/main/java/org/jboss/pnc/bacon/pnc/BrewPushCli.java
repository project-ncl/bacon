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

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.GroupBuildClient;
import org.jboss.pnc.dto.BuildPushResult;
import org.jboss.pnc.dto.requests.BuildPushParameters;
import org.jboss.pnc.dto.requests.GroupBuildPushRequest;
import org.jboss.pnc.enums.BuildPushStatus;
import org.jboss.pnc.restclient.AdvancedBuildClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Command(
        name = "brew-push",
        description = "brew-push",
        subcommands = { BrewPushCli.Build.class, BrewPushCli.GroupBuild.class, BrewPushCli.Status.class, })
public class BrewPushCli {

    private static final ClientCreator<AdvancedBuildClient> BUILD_CREATOR = new ClientCreator<>(
            AdvancedBuildClient::new);
    private static final ClientCreator<GroupBuildClient> GROUP_BUILD_CREATOR = new ClientCreator<>(
            GroupBuildClient::new);

    @Command(
            name = "build",
            description = "Push build to Brew",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc brew-push build 8 --tag-prefix=\"1.0-pnc\"%n%n"
                    + "# To wait for the push to finish, use the '--wait' flag. See the '--timeout' flag also%n"
                    + "$ bacon pnc brew-push build 100 --tag-prefix=\"music-1\" --wait")
    public static class Build extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Id of build")
        private String id;
        @Option(required = true, names = "--tag-prefix", description = "Brew Tag Prefix")
        private String tagPrefix;

        @Option(names = "--reimport", description = "Re-import the build in case it was already imported")
        private boolean reimport = false;

        @Option(names = "--wait", description = "Wait for BrewPush to complete")
        private boolean wait = false;

        @Option(names = "--timeout", description = "Time in minutes the command waits for Group Build completion")
        private String timeout;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (AdvancedBuildClient buildClient = BUILD_CREATOR.newClientAuthenticated()) {
                BuildPushParameters request = BuildPushParameters.builder()
                        .tagPrefix(tagPrefix)
                        .reimport(reimport)
                        .build();

                if (timeout != null) {
                    BuildPushResult bpr = buildClient
                            .executeBrewPush(id, request, Long.parseLong(timeout), TimeUnit.MINUTES);
                    ObjectHelper.print(getJsonOutput(), bpr);
                    return bpr.getStatus() == BuildPushStatus.SUCCESS ? 0 : bpr.getStatus().ordinal();
                }

                if (wait) {
                    BuildPushResult bpr = buildClient.executeBrewPush(id, request).join();
                    ObjectHelper.print(getJsonOutput(), bpr);
                    return bpr.getStatus() == BuildPushStatus.SUCCESS ? 0 : bpr.getStatus().ordinal();
                } else {
                    BuildPushResult bpr = buildClient.push(id, request);
                    ObjectHelper.print(getJsonOutput(), bpr);
                    return bpr.getStatus() == BuildPushStatus.SUCCESS ? 0 : bpr.getStatus().ordinal();
                }
            }
        }
    }

    @Command(
            name = "group-build",
            description = "Push group build to Brew",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc brew-push group-build 8 --tag-prefix=\"1.0-pnc\"%n")
    public static class GroupBuild implements Callable<Integer> {

        @Parameters(description = "Id of group-build")
        private String id;
        @Option(required = true, names = "--tag-prefix", description = "Brew Tag Prefix")
        private String tagPrefix;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            // TODO add wait option for GroupPush
            GroupBuildPushRequest request = GroupBuildPushRequest.builder().tagPrefix(tagPrefix).build();
            try (GroupBuildClient client = GROUP_BUILD_CREATOR.newClientAuthenticated()) {
                client.brewPush(id, request);
                return 0;
            }
        }
    }

    @Command(
            name = "status",
            description = "Brew Push Status",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc brew-push status 10")
    public static class Status extends JSONCommandHandler implements Callable<Integer> {

        @Parameters(description = "Brew Push ID")
        private String id;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (AdvancedBuildClient client = BUILD_CREATOR.newClient()) {
                BuildPushResult bpr = client.getPushResult(id);
                ObjectHelper.print(getJsonOutput(), bpr);
                return bpr.getStatus() == BuildPushStatus.SUCCESS ? 0 : bpr.getStatus().ordinal();
            }
        }
    }
}
