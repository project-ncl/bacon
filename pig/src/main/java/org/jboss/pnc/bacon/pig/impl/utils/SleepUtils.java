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

package org.jboss.pnc.bacon.pig.impl.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 7/7/17
 */
public class SleepUtils {

    private static final Logger log = LoggerFactory.getLogger(SleepUtils.class);

    public static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    public static void waitFor(Supplier<Boolean> condition, int checkInterval, boolean printDot) {
        if (condition.get()) {
            return;
        }
        do {
            sleep(checkInterval);
            if (printDot) {
                System.out.print(".");
            }
        } while (!condition.get());
    }

    /**
     * Wait until the <code>condition</code> returns a non null value or time out is reached
     * 
     * @param condition the condition to evaluate
     * @param checkInterval [seconds] amount of time to wait before attempts
     * @param timeout [seconds] timeout
     * @param ignoreException if set to true will treat condition throwing an exception the same way as returning null
     *        value
     * @param timeoutMessage message in the case of timeout
     * @param <T> return type of the condition
     * @return the value returned by the condition
     *
     * @throws RuntimeException if the timeout is reached
     */
    public static <T> T waitFor(
            Supplier<T> condition,
            int checkInterval,
            long timeout,
            boolean ignoreException,
            String timeoutMessage) {
        int startTime = (int) System.currentTimeMillis() / 1000;
        T value = tryGet(condition, ignoreException);
        if (value != null) {
            return value;
        }
        while (true) {
            sleep(checkInterval);
            value = tryGet(condition, ignoreException);
            if (value != null) {
                return value;
            }
            int currentTime = (int) System.currentTimeMillis() / 1000;
            if (currentTime - startTime > timeout) {
                throw new RuntimeException(timeoutMessage);
            }
        }
    }

    private static <T> T tryGet(Supplier<T> condition, boolean ignoreException) {
        try {
            return condition.get();
        } catch (RuntimeException e) {
            if (ignoreException) {
                log.debug("Ignoring condition evaluation exception", e);
                return null;
            } else {
                throw e;
            }
        }
    }

    private SleepUtils() {
    }
}
