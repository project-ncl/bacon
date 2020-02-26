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

package org.jboss.pnc.bacon.pig.impl.addons.spring;

import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.MRRCSearcher;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.jboss.pnc.bacon.pig.impl.utils.XmlUtils.listNodes;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 7/7/17
 */
public class BomVerifierAddon extends AddOn {

    private static final Logger log = LoggerFactory.getLogger(BomVerifierAddon.class);

    private final MRRCSearcher mrrcSearcher = MRRCSearcher.getInstance();
    private List<GAV> unreleasedWhitelist;

    public BomVerifierAddon(Config config, Map<String, PncBuild> builds, String releasePath, String extrasPath) {
        super(config, builds, releasePath, extrasPath);
    }

    @Override
    protected String getName() {
        return "bomVerifier";
    }

    @Override
    public void trigger() {
        unreleasedWhitelist = readUnreleasedWhitelist();
        List<GAV> unallowedUnreleasedGavs = getUnallowedUnreleasedGavs();
        if (unallowedUnreleasedGavs.size() > 0) {
            throw new RuntimeException(
                    "Unreleased artifacts referenced from BOM found:\n"
                            + StringUtils.join(unallowedUnreleasedGavs, "\n"));
        }
    }

    private List<GAV> readUnreleasedWhitelist() {
        List<String> allowUnreleasedList = getListFromConfig();
        return allowUnreleasedList.stream().map(line -> {
            String[] gav = line.split(":");
            return new GAV(gav[0], gav[1], gav[2], null);
        }).collect(Collectors.toList());
    }

    private List<String> getListFromConfig() {
        try {
            Map<String, ?> config = getConfig();
            if (config != null) {
                List<String> result = (List<String>) config.get("allowUnreleased");
                return result == null ? emptyList() : result;
            }
            return emptyList();
        } catch (Exception e) {
            log.error("While reading allowUnreleased, got the following exception: ", e);
            throw new IllegalArgumentException(
                    "Unable to read the 'allowUnreleased' from bomVerifier addon configuration. Make sure the field contains a list of strings (or is an empty list)",
                    e);
        }
    }

    public List<GAV> getUnallowedUnreleasedGavs() {
        return getDependencyGavs().filter(this::internallyBuilt)
                .filter(this::unreleased)
                .filter(this::notWhitelisted)
                .collect(Collectors.toList());
    }

    private boolean notWhitelisted(GAV gav) {
        return !unreleasedWhitelist.contains(gav);
    }

    private boolean internallyBuilt(GAV gav) {
        return gav.getVersion().contains("redhat");
    }

    private boolean unreleased(GAV gav) {
        return !Boolean.TRUE.equals(mrrcSearcher.isReleased(gav));
    }

    protected Stream<GAV> getDependencyGavs() {
        PncBuild build = builds.get(config.getFlow().getRepositoryGeneration().getSourceBuild());
        File bom = new File("bom");

        build.findArtifactByFileName(config.getFlow().getRepositoryGeneration().getSourceArtifact()).downloadTo(bom);

        List<Node> nodeList = listNodes(bom, "//dependencies/dependency");
        Map<String, String> properties = XmlUtils.getProperties(bom);
        return nodeList.stream().map(node -> GAV.fromXml((Element) node, properties));
    }
}
