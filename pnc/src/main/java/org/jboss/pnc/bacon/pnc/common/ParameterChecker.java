package org.jboss.pnc.bacon.pnc.common;

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
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.enums.RebuildMode;

/**
 * Helper class to check for parameter validity
 */
@Slf4j
public class ParameterChecker {

    /**
     * Check that the rebuild mode provided by the user is valid or not
     */
    public static void checkRebuildModeOption(String rebuildMode) throws FatalException {

        try {
            RebuildMode.valueOf(rebuildMode);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("The rebuild flag contains an illegal option. Possibilities are: ");
            for (RebuildMode mode : RebuildMode.values()) {
                log.error(mode.toString());
            }
            throw new FatalException();
        }
    }
}
