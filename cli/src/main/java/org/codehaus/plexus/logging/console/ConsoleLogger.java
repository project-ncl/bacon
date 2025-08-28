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
package org.codehaus.plexus.logging.console;

import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

/**
 * Massive hack to override the default plexus logging implementation, ConsoleLogger, is adapted to Slf4j, rather than
 * the actual implementation that just prints to stdout
 * <p/>
 * All credit for this genius idea goes to Mr David Walluck (@dwalluck)
 * <p/>
 * Our license generator (snowdrop licenses) uses Maven core under the hood for its business. Unfortunately, the
 * DefaultProjectBuilder from Maven core uses Plexus Logger, with default implementation ConsoleLogger (I couldn't find
 * a way to tell maven to use a different implementation). The ConsoleLogger implementation is rather primitive and just
 * prints to stdout.
 * <p/>
 * This is not OK because we'd like all of our logging to be controlled by slf4j. More specifically, right now we want
 * all of our logs to go to stderr, not stdout.
 * <p/>
 * This re-implementation replaces the default one to use Slf4j for logging instead.
 */
public class ConsoleLogger extends AbstractLogger {

    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConsoleLogger.class);

    public ConsoleLogger() {
        super(1, "console");
    }

    @Override
    public void debug(String s, Throwable throwable) {
        log.debug(s, throwable);
    }

    @Override
    public void info(String s, Throwable throwable) {
        log.info(s, throwable);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        log.warn(s, throwable);
    }

    @Override
    public void error(String s, Throwable throwable) {
        log.error(s, throwable);
    }

    @Override
    public void fatalError(String s, Throwable throwable) {
        log.error(s, throwable);
    }

    @Override
    public Logger getChildLogger(String s) {
        return this;
    }
}
