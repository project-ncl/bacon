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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

/**
 * TODO: resurrect it
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 3/5/18
 */
public class MilestoneNumberFinder {
    public static String getFirstUnused(String url, String version, String milestoneBase) {
        if (!milestoneBase.contains("*")) {
            return milestoneBase;
        }
        if (url == null) {
            throw new RuntimeException(
                    "Invalid configuration. " + "Milestone is configured to be updated incrementally"
                            + " but release storage area url (--releaseStorageUrl) is not provided");
        }
        String milestonePrefix = milestoneBase.replaceAll("\\*", "");

        String indexFile = readIndexFile(url);
        List<Integer> used = findUsedNumbers(version, milestonePrefix, indexFile);
        return milestonePrefix + getOneAbove(used);
    }

    private static Integer getOneAbove(List<Integer> used) {
        return used.stream().max(Integer::compareTo).orElse(0) + 1;
    }

    private static List<Integer> findUsedNumbers(String version, String milestonePrefix, String indexFile) {
        List<Integer> resultList = new ArrayList<>();
        String prefix = version + "." + milestonePrefix;

        String escapedPrefix = prefix.replaceAll("\\.", "\\\\.");
        Pattern pattern = Pattern.compile(escapedPrefix + "\\d+");
        Matcher matcher = pattern.matcher(indexFile);

        while (matcher.find()) {
            String numberAsString = matcher.group().replaceFirst(prefix, "");

            resultList.add(Integer.valueOf(numberAsString));
        }
        return resultList;
    }

    private static String readIndexFile(String url) {
        try {
            File tempFile = File.createTempFile("uploadAreaIndex", "html");
            FileUtils.copyURLToFile(new URL(url), tempFile);
            String content = FileUtils.readFileToString(tempFile, "UTF-8");
            tempFile.delete();
            return content;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download index file from the upload location " + url, e);
        }
    }

    private MilestoneNumberFinder() {
    }
}
