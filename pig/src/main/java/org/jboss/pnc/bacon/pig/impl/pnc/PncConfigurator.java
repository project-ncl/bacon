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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jboss.pnc.bacon.pig.impl.pnc.PncCliParser.parse;
import static org.jboss.pnc.bacon.pig.impl.pnc.PncCliParser.parseList;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/14/17
 */
public class PncConfigurator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd");

    private static final Logger log = LoggerFactory.getLogger(PncConfigurator.class);

    private static final String START_DATE = LocalDateTime.now().format(DATE_FORMAT);
    private static final String END_DATE = LocalDateTime.now().plusDays(1).format(DATE_FORMAT);

    public static Integer getOrGenerateMilestone(Integer versionId,
                                                 String version,
                                                 String milestone,
                                                 String issueTrackerUrl) {
        log.info("Generating milestone for versionId {} and milestone {} in PNC", versionId, milestone);
        Optional<Integer> maybeMilestoneId = getExistingMilestone(versionId, version, milestone);
        return maybeMilestoneId.orElseGet(() -> createMilestone(versionId, milestone, issueTrackerUrl));
    }

    public static void markMilestoneCurrent(Integer versionId, Integer milestoneId) {
        log.info("Making the milestone current");
        String command = String.format("update-product-version %s -cm %d", versionId, milestoneId);
        PncDao.invoke(command);
    }


    public static Optional<Integer> getExistingMilestone(Integer versionId, String version, String milestone) {
        String command = String.format("list-milestones -q productVersion.id==%s", versionId);
        List<String> strings = PncDao.invoke(command, 4);
        List<Map<String, ?>> milestones = parseList(strings);
        return milestones.stream()
                .filter(
                        m -> (version + '.' + milestone).equals(m.get("version"))
                )
                .map(m -> Integer.valueOf(m.get("id").toString()))
                .findAny();
    }

    private static Integer createMilestone(Integer versionId, String milestone, String issueTrackerUrl) {
        String command = String.format("create-milestone %s %s %s %s %s",
                versionId, milestone, START_DATE, END_DATE,
                issueTrackerUrl
        );
        Map<String, ?> milestoneData = parse(PncDao.invoke(command));
        return Integer.valueOf(milestoneData.get("id").toString());
    }

    private PncConfigurator() {
    }
}
