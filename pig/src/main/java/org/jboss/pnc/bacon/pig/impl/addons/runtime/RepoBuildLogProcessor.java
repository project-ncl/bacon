/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pig.impl.addons.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/5/17
 */
public class RepoBuildLogProcessor {
    private static final String START_TAG = "swarm_repository_listing_begin";
    private static final String END_TAG = "swarm_repository_listing_end";

    public static List<String> getList(String header,
                                       List<String> buildLog) {
        Iterator<String> logIterator = buildLog.iterator();
        while (logIterator.hasNext()) {
            if (header.equals(logIterator.next())) {
                break;
            }
        }
        if (logIterator.hasNext()) {
            if (START_TAG.equals(logIterator.next()) && logIterator.hasNext()) {
                return takeUntilTheEnd(logIterator);
            } else {
                throw new IllegalArgumentException("Malformed repo log listing for header: " + header +
                        " in the log: " + join(buildLog, "\n"));
            }
        } else {
            throw new IllegalArgumentException("Unable to find " + header + " in the log: " + join(buildLog, "\n"));
        }
    }

    private static List<String> takeUntilTheEnd(Iterator<String> logIterator) {
        List<String> resultList = new ArrayList<>();
        while (logIterator.hasNext()) {
            String nextLine = logIterator.next();
            if (END_TAG.equals(nextLine)) {
                break;
            }
            resultList.add(nextLine);
        }
        return resultList;
    }

    private RepoBuildLogProcessor() {
    }
}
