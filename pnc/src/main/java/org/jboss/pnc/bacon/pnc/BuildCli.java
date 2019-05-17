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
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.BuildClient;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@GroupCommandDefinition(name = "build", description = "build", groupCommands = {
        BuildCli.DownloadSources.class
})
public class BuildCli extends AbstractCommand {


    private BuildClient client = new BuildClient(PncClientHelper.getPncConfiguration());

    @CommandDefinition(name = "download-sources", description = "Download SCM sources used for the build")
    public class DownloadSources extends AbstractCommand {

        @Argument(required = true, description = "Id of build")
        private int id;


        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                String filename = id + "-sources.tar.gz";

                Response response = client.getInternalScmArchiveLink(id);

                InputStream in = (InputStream) response.getEntity();

                Path path = Paths.get(filename);

                try {
                    Files.copy(in, path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
