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
package org.jboss.pnc.bacon.cli;

import lombok.extern.slf4j.Slf4j;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandRuntime;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.RequiredOptionException;
import org.aesh.command.registry.CommandRegistry;
import org.jboss.bacon.da.Da;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.pig.Pig;
import org.jboss.pnc.bacon.pnc.Pnc;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */


@Slf4j
@GroupCommandDefinition(
        name = "bacon.jar",
        description = "Bacon CLI",
        groupCommands = {Pnc.class, Da.class, Pig.class})
public class App extends AbstractCommand {

    public void run(String[] args) throws Exception {

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(this.getClass())
                .create();

       CommandRuntime runtime = AeshCommandRuntimeBuilder
                .builder()
                .commandRegistry(registry)
                .build();

       try {
           runtime.executeCommand(buildCLIOutput(args));
       } catch (RequiredOptionException ex) {
           log.error("Missing argument/option: {}", ex.getMessage());
           System.exit(1);
       } catch (RuntimeException ex) {
           log.error(ex.getMessage());

           // if stacktrace not thrown from aesh
           if (!ex.getCause().getClass().getCanonicalName().contains("aesh")) {
               log.error("Stacktrace", ex);
           }

           // signal that an error has occurred
           System.exit(1);
       }
    }

    private static String buildCLIOutput(String[] args) {
        StringBuilder builder = new StringBuilder();

        builder.append("bacon.jar ");

        for (String opt : args) {
            // if opt contains spaces, that means it was grouped inside quotation marks
            // we have to pass the same quotation marks to bacon (it's removed in the args array)
            if (opt.contains(" ")) {
                builder.append("\"");
                builder.append(opt);
                builder.append("\" ");
            } else {
                builder.append(opt);
                builder.append(" ");
            }
        }

        return builder.toString();
    }

    public static void main(String[] args) throws Exception {

        App app = new App();
        app.run(args);
    }
}
