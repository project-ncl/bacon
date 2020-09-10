/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.common.cli;

import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;
import picocli.CommandLine.Option;

import java.util.Collection;

/**
 * Class used to provide a default implementation for List* type commands.
 *
 * The subclass only needs to implement the 'getAll' method to teach it how to retrieve all the contents
 *
 * @author jbrazdil
 */
public abstract class AbstractBuildListCommand extends AbstractListCommand<Build> {

    @Option(names = "--latest", description = "Get only the latest build.")
    private boolean latest;

    @Option(names = "--running-only", description = "Get only running builds.")
    private boolean running;

    public Collection<Build> getAll(String sort, String query) throws RemoteResourceException {
        BuildsFilterParameters filter = new BuildsFilterParameters();
        filter.setLatest(latest);
        filter.setRunning(running);
        return getAll(filter, sort, query);
    }

    public abstract Collection<Build> getAll(BuildsFilterParameters buildsFilter, String sort, String query)
            throws RemoteResourceException;
}
