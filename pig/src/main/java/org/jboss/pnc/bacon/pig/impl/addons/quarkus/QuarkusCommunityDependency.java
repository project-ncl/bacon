package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CommunityDependency;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CsvExportable;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.util.Collection;
import java.util.stream.Collectors;

public class QuarkusCommunityDependency implements CsvExportable {

    private final String usingExtensions;
    private final CommunityDependency communityDependency;

    public QuarkusCommunityDependency(Collection<GAV> usingExtensions, CommunityDependency communityDependency) {
        this.usingExtensions = usingExtensions.stream().map(gav -> {
            if (gav.getGroupId().equals("io.quarkus")) {
                return gav.getArtifactId();
            } else {
                return String.format("%s:%s", gav.getGroupId(), gav.getArtifactId());
            }
        }).collect(Collectors.joining(","));
        this.communityDependency = communityDependency;
    }

    @Override
    public String toCsvLine() {
        return String.format("%s;%s", communityDependency.toCsvLine(), usingExtensions);
    }
}
