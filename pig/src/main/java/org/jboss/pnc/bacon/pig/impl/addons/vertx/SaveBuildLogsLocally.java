/*
 * JBoss, Home of Professional Open Source. Copyright 2017 Red Hat, Inc., and individual
 * contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jboss.pnc.bacon.pig.impl.addons.vertx;

import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

/**
 * @author Paul Gallagher, pgallagh@redhat.com <br>
 *         Date: 2022-07-28
 *
 *         Daves the build logs for PNC for all the projects in the PiG build group to a local directory under `extras`
 *         so they can be analysed if needed, very usefull if you have multiple builds in a build group
 */
public class SaveBuildLogsLocally extends AddOn {

    private static final Logger log = LoggerFactory.getLogger(SaveBuildLogsLocally.class);

    public SaveBuildLogsLocally(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    public String getName() {
        return "saveBuildLogsLocally";
    }

    @Override
    public void trigger() {
        File logDir = new File(extrasPath + "build-logs");
        String fileName = "";
        log.info("Running SaveBuildLogsLocally - logs are in {}", logDir);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        try {
            for (PncBuild build : builds.values()) {
                List<String> bcLog = build.getBuildLog();
                fileName = logDir + File.separator + build.getName() + ".txt";
                FileWriter writer = new FileWriter(fileName);
                for (String str : bcLog) {
                    writer.write(str + System.lineSeparator());
                }
                writer.close();
            }
        } catch (java.io.IOException e) {
            log.error("Writing build log {}", fileName, e);
            return;
        }
    }
}
