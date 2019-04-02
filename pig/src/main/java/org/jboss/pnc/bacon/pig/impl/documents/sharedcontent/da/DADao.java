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
package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.pnc.bacon.pig.impl.utils.OSCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/2/17
 */
public class DADao {
    private static final Logger log = LoggerFactory.getLogger(DADao.class);

    private static final String DA_CLI_PY = "da-cli.py";

    private static final String DA_CLI_CMD = getDACLICommand();

    private static String runCommandReturningSingleLine(String command) {
        List<String> result = OSCommandExecutor.runCommand(command);

        if (result.size() != 1) {
            throw new IllegalStateException("Failed to run command: " + command + ", result: " + result);
        }

        return result.get(0);
    }

    private static String getCygwinHome() {
        String command = "cmd /c \"reg QUERY HKLM\\Software\\Cygwin\\setup /v rootdir /t REG_SZ | findstr REG_SZ | for /f \\\"tokens=3*\\\" %f in ('more') do @echo %f\"";
        String cygwinHome = runCommandReturningSingleLine(command);

        if (cygwinHome.startsWith("ERROR: ")) {
            throw new IllegalStateException("Unable to find Cygwin installation");
        }

        cygwinHome = StringUtils.stripEnd(cygwinHome, " ");
        cygwinHome = FilenameUtils.separatorsToUnix(cygwinHome);

        Path cygwinHomePath = Paths.get(cygwinHome);

        if (!Files.isDirectory(cygwinHomePath, new LinkOption[] {})) {
            throw new IllegalStateException("Unable to find Cygwin installation");
        }

        return cygwinHomePath.toString();
    }

    private static String findInWindowsPath(String cygwinHome, String executable) {
        String command = "where " + executable;
        String result = runCommandReturningSingleLine(command);

        return result;
    }

    private static Path convertToWindowsPath(String cygwinHome, String unixPath) {
        String command = cygwinHome + "/bin/cygpath --mixed \"" + (unixPath.startsWith("/") ? unixPath : findInWindowsPath(cygwinHome, unixPath)) + "\"";
        String result = runCommandReturningSingleLine(command);
        Path convertedPath = Paths.get(result);

        return convertedPath;
    }

    private static String getDACLICommand() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            return DA_CLI_PY;
        }

        String cygwinHome = getCygwinHome();
        Path pythonPath = convertToWindowsPath(cygwinHome, "/usr/bin/python3");

        if (!Files.isRegularFile(pythonPath) || !Files.isExecutable(pythonPath)) {
            throw new IllegalStateException("Path is not an executable file: " + pythonPath);
        }

        Path scriptPath = convertToWindowsPath(cygwinHome, DA_CLI_PY);

        if (!Files.isRegularFile(scriptPath)) {
            throw new IllegalStateException("Path is not a file: " + scriptPath);
        }

        String command = pythonPath + " " + scriptPath;

        return command;
    }

    public void fillDaData(CommunityDependency dependency) {
        log.debug("Starting analysis for: {}", dependency);
        List<String> result =
                OSCommandExecutor.runCommand(DA_CLI_CMD + " lookup " + dependency.toGav());
        result = result.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if (result.size() != 1) {
            log.error("Invalid number of result lines ({}) for lookup for {}", result.size(), dependency.toGav());
            log.error("Output: {}", StringUtils.join(result, '\n'));
            throw new IllegalStateException("Failed to fill DA data. See log for more details");
        }
        String[] splitResult = result.get(0).split("\\s+");
        String proposed = splitResult[1];
        // ignored for now
        //String state = splitResult[2];
        String available = splitResult.length > 3 ? splitResult[3].trim() : null;

        if (!"None".equals(proposed)) {
            dependency.setState(DependencyState.MATCH_FOUND);
            dependency.setRecommendation(proposed);
            dependency.setAvailableVersions(available);
        } else if (StringUtils.isNotEmpty(available)) {
            dependency.setState(DependencyState.REVERSION_POSSIBLE);
            dependency.setAvailableVersions(available);
        } else {
            dependency.setState(DependencyState.NO_MATCH);
            dependency.setAvailableVersions("None");
        }
        log.debug("Done for: {}", dependency);
    }

    public List<DAListArtifact> getWhitelist() {
        List<String> listAsStrings = OSCommandExecutor.runCommand(DA_CLI_CMD + " list white");
        return listAsStrings.stream()
                .map(DAListArtifact::new)
                .collect(Collectors.toList());
    }

    public DADao() {
        verifyDaCli();
    }

    private void verifyDaCli() {
        OSCommandExecutor.CommandExecutor executor =
                OSCommandExecutor.executor(DA_CLI_CMD + " lookup junit:junit:4.12").exec();
        int status = executor.getStatus();
        if (status != 0) {
            log.error("{} is not configured properly or network/VPN is not functional. Exiting", DA_CLI_CMD);
            log.error("Test lookup failed with:");
            log.error(executor.joinedOutput());
            throw new IllegalStateException("DA test query failed");
        }
    }

    private static final DADao instance = new DADao();

    public static DADao getInstance() {
        return instance;
    }
}
