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

import org.jboss.pnc.bacon.common.SubCommandHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "build-config", mixinStandardHelpOptions = true)
public class BuildConfiguration extends SubCommandHelper {

    @CommandLine.Command(name = "create", mixinStandardHelpOptions = true)
    public void create() {
    }

    @CommandLine.Command(name = "get", mixinStandardHelpOptions = true)
    public void get() {
    }

    @CommandLine.Command(name = "list", mixinStandardHelpOptions = true)
    public void list() {
    }

    @CommandLine.Command(name = "update", mixinStandardHelpOptions = true)
    public void update() {
    }

    @CommandLine.Command(name = "delete", mixinStandardHelpOptions = true)
    public void delete() {
    }
}
