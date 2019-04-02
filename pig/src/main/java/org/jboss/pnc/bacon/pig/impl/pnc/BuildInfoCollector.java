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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jboss.pnc.bacon.pig.impl.pnc.PncCliParser.parse;
import static org.jboss.pnc.bacon.pig.impl.pnc.PncCliParser.parseList;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/3/17
 */
public class BuildInfoCollector {
    private static final Logger log = LoggerFactory.getLogger(BuildInfoCollector.class);

//    public static Map<String, BuildData> retrieveBuildDataByConfigName() {
//        List<String> buildConfigIds = getBuildConfigIds();
//        log.info("Will fetch build records for build configs: {}", buildConfigIds);
//
//        Map<String, BuildData> buildsByName = buildConfigIds.parallelStream()
//                .map(this::getBuildDataForConfigId)
//                .collect(Collectors.toMap(BuildData::getName, Function.identity()));
//        buildsByName.values().forEach(BuildInfoCollector::addBuiltArtifacts);
//        return buildsByName;
//    }

    public static PncBuild getBuildData(Integer buildId) {
        return getBuildData(buildId, false);
    }

    public static PncBuild getBuildData(Integer buildId, boolean full) {
        String command = String.format("get-build-record %d", buildId);
        List<String> recordAsString = PncDao.invoke(command, 4);
        Map<String, ?> buildRecord = parse(recordAsString);
        PncBuild result = new PncBuild(buildRecord);
        if (full) {
            addBuiltArtifacts(result);
            addBuildLog(result);
        }
        return result;
    }

    public static void addBuildLog(PncBuild bd) {
        String command = String.format("get-log-for-record %d", bd.getId());
        bd.setBuildLog(
                PncDao.invoke(command, 4)
        );
    }

    public static void addBuiltArtifacts(PncBuild bd) {
        String command = String.format("list-built-artifacts %d -p 10000", bd.getId());
        bd.setBuiltArtifacts(
                parseList(PncDao.invoke(command, 4))
        );
    }

    public static void addDependencies(PncBuild bd) {
        int pageIndex = 0;

        List<Map<String, ?>> artifactsAsMaps = new ArrayList<>();
        while (true) {
            String command = String.format("list-dependency-artifacts %d -p 50 --page-index %d", bd.getId(), pageIndex++);
            List<String> output = PncDao.invoke(command, 4);

            if (output.isEmpty()) {
                break;
            } else {
                artifactsAsMaps.addAll(parseList(output));
            }
        }

        bd.setDependencyArtifacts(artifactsAsMaps);
    }

    public static PncBuild getLatestBuild(Integer configId) {
        String command = String.format("list-records-for-build-configuration -i %d --sort '=desc=id' --page-size 1 -q 'status==SUCCESS'", configId);
        return PncDao.invokeAndParseList(command, 4)
                .stream()
                .map(PncBuild::new)
                .peek(BuildInfoCollector::addBuiltArtifacts)
                .peek(BuildInfoCollector::addBuildLog)
                .findAny()
                .orElseThrow(() -> new NoSuccessfulBuildException(configId));
    }

    private BuildInfoCollector() {
    }
}
