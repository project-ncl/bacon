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
package org.jboss.pnc.bacon.common.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;
import picocli.CommandLine.Option;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Class used to provide a default implementation for List* type commands.
 *
 * The subclass only needs to implement the 'getAll' method to teach it how to retrieve all the contents
 *
 * @param <T>
 */
@Slf4j
public abstract class AbstractListCommand<T> extends JSONCommandHandler implements Callable<Integer> {

    @Option(names = "--sort", description = "Sort order (using RSQL)")
    private String sort;

    @Option(names = "--query", description = "Query parameter (using RSQL)")
    private String query;

    protected boolean print = true;

    @Override
    public Integer call() {
        if (query == null && sort == null && print == true) {
            log.warn("Listing entities without filters may take some time, please be patient.");
        }
        try {
            ObjectHelper.print(getJsonOutput(), getAll(sort, query));
        } catch (JsonProcessingException | ClientException e) {
            throw new FatalException("Caught exception", e);
        }
        return 0;
    }

    public abstract Collection<T> getAll(String sort, String query) throws RemoteResourceException;
}
