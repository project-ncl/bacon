/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.addons.runtime;

import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CommunityDependency;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CsvExportable;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.ThorntailCommunityDependency;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
public class ThorntailDepAnalyzer implements Function<List<CommunityDependency>, List<? extends CsvExportable>> {
    private final List<String> downloadedForSwarm;

    public ThorntailDepAnalyzer(List<String> swarmLog) {
        downloadedForSwarm = swarmLog.stream()
                .filter(line -> line.startsWith("Downloaded"))
                .filter(line -> line.contains(".jar"))
                // lines are of the form: Downloaded: http://... (some add. info)
                .map(l -> l.split("\\s+")[1])
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<ThorntailCommunityDependency> apply(List<CommunityDependency> communityDependencies) {
        return communityDependencies.parallelStream()
                .map(this::toThorntailCommunityDependency)
                .collect(Collectors.toList());
    }

    private ThorntailCommunityDependency toThorntailCommunityDependency(CommunityDependency communityDependency) {
        List<String> swarmBuildDownloads = downloadedForSwarm.stream()
                .filter(d -> d.contains(communityDependency.toPathSubstring()))
                .map(l -> l.substring(l.lastIndexOf("/") + 1))
                .collect(Collectors.toList());
        return new ThorntailCommunityDependency(communityDependency, swarmBuildDownloads);
    }
}
