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
package org.jboss.pnc.bacon.pig.impl.pnc;

import org.jboss.pnc.bacon.pig.impl.utils.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/14/17
 */
public class PncBuilder {
    private static final Logger log = LoggerFactory.getLogger(PncBuilder.class);

    public void buildAndWait(Integer groupId, boolean tempBuild, boolean tempBuildTS, String rebuildMode) {
        log.info("Performing builds of build group {} in PNC", groupId);
        int previousGroupBuildId = getPreviousBuildId(groupId);
        triggerGroupBuild(groupId, tempBuild, tempBuildTS, rebuildMode);
        int newBuildId = getCurrentBuildId(groupId, previousGroupBuildId);
        waitForSuccessfulFinish(groupId, newBuildId);
    }

    private void triggerGroupBuild(Integer groupId, boolean tempBuild, boolean tempBuildTS, String rebuildMode) {
        String temp;
        String ts;
        String command = String.format("build-set %s %s --rebuild-mode %s -i %d",
                temp = tempBuild ? "--temporary-build" : "",
                ts = tempBuildTS ? "--timestamp-alignment" : "",
                rebuildMode,
                groupId );
        PncDao.invoke(command);
    }

    private int getCurrentBuildId(Integer groupId, int previousGroupBuildId) {
        return getNewestGroupBuildId(groupId)
                .filter(buildId -> buildId > previousGroupBuildId)
                .orElseThrow(() -> new RuntimeException("Build group for group " + groupId + " not started properly"));
    }

    private int getPreviousBuildId(Integer groupId) {
        return getNewestGroupBuildId(groupId).orElse(0);
    }

    private Optional<Integer> getNewestGroupBuildId(Integer groupId) {
        // issue a command that only returns one result, which is the latest build set record
        final String command = String
            .format("list-build-set-records -i %d --sort '=desc=id' -p 1", groupId);

        // use the standard infrastructure even though we now there is only result
        // done in order to avoid adding too many methods to PncDao
        return PncDao.invokeAndGetResultIds(command, 4)
                .stream().max(Integer::compareTo);
    }

    private static void waitForSuccessfulFinish(int groupId, int groupBuildId) {
        log.info("waiting for finish of group build {}", groupBuildId);
        SleepUtils.waitFor(() -> isSuccessfullyFinished(groupId, groupBuildId), 30);
        log.info("group build finished successfully");
    }

    private static Boolean isSuccessfullyFinished(int groupId, int groupBuildId) {
        Map<String, ?> groupBuildRecord = getGroupBuild(groupId, groupBuildId);
        String status = (String) groupBuildRecord.get("status");
        switch (status) {
            case "BUILDING": return false;
            case "REJECTED": //PNC has already built the project and returned "SUCCESS"
            case "SUCCESS": return true;
            default: throw new RuntimeException("Build group failed " + groupBuildRecord);
        }
    }

    private static Map<String, ?> getGroupBuild(int groupId, int groupBuildId) {
        String command = String.format("list-build-set-records -i %d -q 'id==%d'", groupId, groupBuildId);
        return PncDao.invokeAndParseList(command, 4)
                .iterator().next();
    }

}
