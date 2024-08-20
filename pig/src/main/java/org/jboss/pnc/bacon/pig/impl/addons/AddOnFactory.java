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
package org.jboss.pnc.bacon.pig.impl.addons;

import org.jboss.pnc.bacon.pig.impl.addons.camel.CamelRuntimeDependenciesToAlignTree;
import org.jboss.pnc.bacon.pig.impl.addons.camel.RuntimeDependenciesToAlignTree;
import org.jboss.pnc.bacon.pig.impl.addons.cachi2.Cachi2LockfileAddon;
import org.jboss.pnc.bacon.pig.impl.addons.microprofile.MicroProfileSmallRyeCommunityDepAnalyzer;
import org.jboss.pnc.bacon.pig.impl.addons.quarkus.QuarkusCommunityDepAnalyzer;
import org.jboss.pnc.bacon.pig.impl.addons.quarkus.QuarkusPostBuildAnalyzer;
import org.jboss.pnc.bacon.pig.impl.addons.quarkus.VertxArtifactFinder;
import org.jboss.pnc.bacon.pig.impl.addons.rhba.OfflineManifestGenerator;
import org.jboss.pnc.bacon.pig.impl.addons.runtime.RuntimeDependenciesAnalyzer;
import org.jboss.pnc.bacon.pig.impl.addons.scanservice.PostBuildScanService;
import org.jboss.pnc.bacon.pig.impl.addons.spring.BomVerifierAddon;
import org.jboss.pnc.bacon.pig.impl.addons.vertx.FindTransitiveDuplicateArtifactsInDepTree;
import org.jboss.pnc.bacon.pig.impl.addons.vertx.NotYetAlignedFromDependencyTree;
import org.jboss.pnc.bacon.pig.impl.addons.vertx.SaveBuildLogsLocally;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/11/17
 */
public class AddOnFactory {

    public static List<AddOn> listAddOns(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath,
            Deliverables deliverables) {
        ArrayList<AddOn> resultList = new ArrayList<>();

        resultList.add(new RuntimeDependenciesAnalyzer(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new ExtraDeliverableDownloader(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new BomVerifierAddon(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new CamelRuntimeDependenciesToAlignTree(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new RuntimeDependenciesToAlignTree(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new NotYetAlignedFromDependencyTree(pigConfiguration, builds, releasePath, extrasPath));
        resultList
                .add(new QuarkusCommunityDepAnalyzer(pigConfiguration, builds, releasePath, extrasPath, deliverables));
        resultList.add(new MicroProfileSmallRyeCommunityDepAnalyzer(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new QuarkusPostBuildAnalyzer(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new OfflineManifestGenerator(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new VertxArtifactFinder(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new PostBuildScanService(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(new SaveBuildLogsLocally(pigConfiguration, builds, releasePath, extrasPath));
        resultList.add(
                new FindTransitiveDuplicateArtifactsInDepTree(
                        pigConfiguration,
                        builds,
                        releasePath,
                        extrasPath,
                        deliverables));
        resultList.add(new Cachi2LockfileAddon(pigConfiguration, builds, releasePath, extrasPath));
        return resultList;
    }

    private AddOnFactory() {
    }
}
