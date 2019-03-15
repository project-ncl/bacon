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
import org.aesh.command.GroupCommandDefinition;
import org.jboss.pnc.bacon.common.SubCommandHelper;

@GroupCommandDefinition(
        name = "generate",
        description = "Further tools to generate artifacts",
        groupCommands = {
                GenerateTools.RepoList.class,
                GenerateTools.GenerateSourcesZip.class,
                GenerateTools.MakeMead.class
        })
public class GenerateTools extends SubCommandHelper {

    @CommandDefinition(name = "repo-list", description = "Generate a repo-list")
    public class RepoList extends SubCommandHelper {
        // TODO: should this exist given PIG might implement it also?
    }

    @CommandDefinition(name = "sources-zip", description = "Generate a sources zip")
    public class GenerateSourcesZip extends SubCommandHelper {
        // TODO: should this exist given PIG might implement it also?
    }

    @CommandDefinition(name = "make-mead", description = "make-mead")
    public class MakeMead extends SubCommandHelper {
    }
}
