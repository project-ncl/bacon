/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.pig.impl.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 8/23/17
 */
public class LoggerUtils {
    // TODO this appears to be broken!
    public static void setVerboseLogging(boolean verbose) {
        if (verbose) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = lc.getLoggerList().iterator().next();
            List<Filter<ILoggingEvent>> filters = logger.getAppender("STDOUT").getCopyOfAttachedFiltersList();
            filters.stream()
                    .filter(LevelFilter.class::isInstance)
                    .forEach(filter -> ((LevelFilter) filter).setLevel(Level.DEBUG));
            System.out.println("Enabled debug logging");
        }
    }

    private LoggerUtils() {
    }
}
