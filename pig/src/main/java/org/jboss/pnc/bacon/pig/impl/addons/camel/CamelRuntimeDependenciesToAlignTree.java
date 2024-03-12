/*
 * JBoss, Home of Professional Open Source. Copyright 2022 Red Hat, Inc., and individual
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
package org.jboss.pnc.bacon.pig.impl.addons.camel;

import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tom Cunningham, tcunning@redhat.com <br>
 * @author Paul Gallagher, pgallagh@redhat.com <br>
 *         Date: 2018-11-20
 *
 *         If you run your builds with 'dependency:tree' then you can use this addon to give you the list of compile
 *         scope dependencies that are not redhat builds
 */
public class CamelRuntimeDependenciesToAlignTree extends AddOn {

    private static final Logger log = LoggerFactory.getLogger(CamelRuntimeDependenciesToAlignTree.class);

    public CamelRuntimeDependenciesToAlignTree(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    public String getName() {
        return "camelRuntimeDependenciesToAlignTree";
    }

    private int indexOfPattern(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.start();
        }
        return -1;
    }

    private String parseDependency(String line) {
        String dependency = line.replace(":runtime", "").replace(":compile", "");
        dependency = dependency.replace("[INFO] ", "");
        dependency = dependency.replaceFirst("([+-|\\s]+\\s+)", "");
        return dependency;
    }

    @Override
    public void trigger() {
        String filename = extrasPath + "DependenciesToAlignTree.txt";
        String buildFromSourceStatsFileName = extrasPath + "CamelBuildFromSourceStats.txt";
        log.info("Running CamelDependenciesToAlignTree - report is {}", filename);

        try (PrintWriter file = new PrintWriter(filename, StandardCharsets.UTF_8.name());
                PrintWriter buildFromSourceStatsFile = new PrintWriter(
                        buildFromSourceStatsFileName,
                        StandardCharsets.UTF_8.name())) {
            for (PncBuild build : builds.values()) {
                // Make a unique list so we don't get multiples from
                // sub-module's dependency tree list
                List<String> bcLog = build.getBuildLog();

                file.print("-------- [" + build.getId() + "] " + build.getName() + " --------\n");
                buildFromSourceStatsFile.print("-------- [" + build.getId() + "] " + build.getName() + " --------\n");

                List<String> runtimeDeps = new ArrayList<String>();
                PrintWriter camelBuildFile = new PrintWriter(
                        new String(extrasPath + build.getName() + "-camelProjectDependencies.txt"),
                        StandardCharsets.UTF_8.name());

                TreeParser treeparser = new TreeParser();
                ArrayList<TreeNode> al = treeparser.parse(bcLog);

                ArrayList<String> dependencies = treeparser.collectFirstLevelDependencies(al);
                Set<String> uniqueDependencies = new HashSet<String>(dependencies);
                ArrayList<String> filterForScope = new ArrayList<String>();
                for (String dep : uniqueDependencies) {
                    if (!dep.endsWith(":test")) {
                        filterForScope.add(dep);
                    }
                }

                // Print the unique first level dependencies out to a file
                camelBuildFile.println("Unique, first level non-test-scope dependencies");
                for (String uniqueDep : filterForScope) {
                    camelBuildFile.println(uniqueDep);
                }
                camelBuildFile.flush();
                camelBuildFile.close();

                buildFromSourceStatsFile.println("Number of unique dependencies: " + filterForScope.size());

                int prodCount = 0;
                for (String prod : filterForScope) {
                    if (prod.contains(".redhat")) {
                        prodCount++;
                    }
                }

                // Print out the productization stats
                buildFromSourceStatsFile.println(
                        "Found " + prodCount + " unique productized dependencies, " + filterForScope.size()
                                + " total dependencies\n");
                float pct = (float) prodCount / filterForScope.size();
                buildFromSourceStatsFile
                        .print("Build from source percentage : " + String.format("%.2f", pct * 100) + "%\n");
            }

            log.info("Finished writing CamelDependenciesToAlignTree - report is {}", filename);

            file.flush();
            buildFromSourceStatsFile.flush();
        } catch (Exception e) {
            log.error("Error while creating RuntimeDependenciesToAlignTree report", e);
        }
    }
}
