package org.jboss.pnc.bacon.pig.impl.addons.runtime;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CommunityDependency;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CsvExportable;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.ThorntailCommunityDependency;

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
