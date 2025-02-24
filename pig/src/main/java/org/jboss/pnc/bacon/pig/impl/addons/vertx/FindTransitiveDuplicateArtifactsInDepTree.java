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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul Gallagher, pgallagh@redhat.com <br>
 *         Date: 2022-07-28
 *
 *         If you run your builds with 'dependency:tree' then you can use this addon to give you the list of where the
 *         duplicate artifacts come into your transitive tree
 */
public class FindTransitiveDuplicateArtifactsInDepTree extends AddOn {

    private static final Logger log = LoggerFactory.getLogger(FindTransitiveDuplicateArtifactsInDepTree.class);
    private static Deliverables deliverables;
    private static final String startTreeSectionPattern = new String(
            "\\[INFO\\] --- maven-dependency-plugin:.*:tree.*");
    private static final Pattern pattern = Pattern.compile(startTreeSectionPattern);
    private static final String[] endTreeSection = {
            "[INFO] ",
            "[INFO] ------------------------------------------------------------------------" };
    private static final String topLevelEntry = "[INFO] +";

    private static List<GAV> duplicates = new ArrayList<GAV>();

    private static String duplicatesString = "";
    private static String entryPoint = "";
    private static boolean dupFound = false;
    private static PrintWriter outputFile = null;

    public FindTransitiveDuplicateArtifactsInDepTree(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath,
            Deliverables deliverables) {
        super(pigConfiguration, builds, releasePath, extrasPath);
        this.deliverables = deliverables;
    }

    @Override
    public String getName() {
        return "findTransitiveDuplicateArtifactsInDepTree";
    }

    private boolean isTopLevelEntry(String line) {
        boolean topLevel = false;
        if (line.startsWith(topLevelEntry)) {
            topLevel = true;
        }
        return topLevel;
    }

    private boolean isStartTree(String line) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find();
    }

    private boolean isEndTree(String line) {
        boolean end = false;
        for (String s : endTreeSection) {
            if (line.equals(s)) {
                end = true;
                break;
            }
        }
        return end;
    }

    private void findDuplicates(ListIterator<String> itr) {
        String bcLine = itr.next().replace(" (optional)", "");
        // Just rewind the stack if it is the end of the tree
        if (isEndTree(bcLine)) {
            return;
        }

        // If it is not a redhat artifact ignore it
        if (bcLine.contains("redhat-")) {
            String[] bits = bcLine.split(" ");
            // if its a first line dep which means record it as the entry point
            if (isTopLevelEntry(bcLine)) {
                entryPoint = bits[bits.length - 1];
            }

            // Still need top check if it is a DUP or not and add to the dup list
            String[] gavParts = bits[bits.length - 1].split(":");
            GAV gav = new GAV(gavParts[0], gavParts[1], gavParts[3], null);
            if (duplicates.contains(gav)) {
                duplicatesString = duplicatesString + "      " + gav.toGav() + "\n";
                dupFound = true;
            }

            // We are back at the top level and a DUP has been found so record and reset
            if (isTopLevelEntry(bcLine) && dupFound) {
                outputFile.println("Top Level Dependency : " + entryPoint);
                outputFile.println("  Possible Duplicates  : ");
                outputFile.println(duplicatesString);
                // Reset as we are back at the top and will move to the next top level dep or
                // will be the end and we leave and search for the next dep:tree section in log
                dupFound = false;
                entryPoint = "";
                duplicatesString = "";
            }
        }
        // call again to move down the tree, even if it is not a redhat artifact it won't matter
        // as nothing below will be so just continue to next line anyway
        findDuplicates(itr);
    }

    @Override
    public void trigger() {
        String filename = extrasPath + "DuplicateArtifactLocations.txt";
        log.info("Running FindTransitiveDuplicateArtifactsInDepTree - report is {}", filename);

        String duplicatesPath = extrasPath + deliverables.getDuplicateArtifactListName();
        try (BufferedReader br = new BufferedReader(new FileReader(duplicatesPath))) {
            String gavLine;
            while ((gavLine = br.readLine()) != null) {
                // Should be a GAV so load into list
                duplicates.add(GAV.fromColonSeparatedGAV(gavLine.trim()));
            }
        } catch (Exception e) {
            log.error("Required duplicates file problem {}", duplicatesPath, e);
            return;
        }

        // Iterate the file until we find the start sectiom
        try {
            outputFile = new PrintWriter(filename, StandardCharsets.UTF_8.name());
            for (PncBuild build : builds.values()) {
                List<String> bcLog = build.getBuildLog();
                outputFile.println("-------- [ " + build.getId() + " ] " + build.getName() + " --------");
                ListIterator<String> itr = bcLog.listIterator();
                while (itr.hasNext()) {
                    String bcLine = itr.next();
                    if (isStartTree(bcLine)) {
                        // Move on the first line to get to the start of the real tree
                        itr.next();
                        // Make sure there is a tree as no deps like a BOM will just move into
                        // something else, check the next then move back so it can be checked as a dup
                        if (isTopLevelEntry(itr.next())) {
                            itr.previous();
                            findDuplicates(itr);
                        }
                    }
                }
                outputFile.println();
            }
            outputFile.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error("Creating FindTransitiveDuplicateArtifactsInDepTree report {}", e);
        }
    }
}
