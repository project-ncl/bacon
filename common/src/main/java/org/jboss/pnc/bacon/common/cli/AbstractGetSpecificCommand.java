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

import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.client.ClientException;

import com.fasterxml.jackson.core.JsonProcessingException;

import picocli.CommandLine.Parameters;

/**
 * Class used to get specific item from PNC. The subclass only has to implement 'getSpecific' to teach it how to
 * retrieve the specific item
 *
 * @param <T>
 */
public abstract class AbstractGetSpecificCommand<T> extends JSONCommandHandler implements Callable<Integer> {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractGetSpecificCommand.class);
    @Parameters(description = "Id of item")
    private String id;

    @Override
    public Integer call() {
        try {
            ObjectHelper.print(getJsonOutput(), getSpecific(id));
        } catch (JsonProcessingException | ClientException e) {
            throw new FatalException("Caught exception " + e.getMessage(), e);
        }
        return 0;
    }

    public abstract T getSpecific(String id) throws ClientException;
}
