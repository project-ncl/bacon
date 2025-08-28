package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
public class ThorntailCommunityDependency implements CsvExportable {
    private final CommunityDependency communityDependency;
    private List<String> usedForThorntail;

    public ThorntailCommunityDependency(CommunityDependency communityDependency, List<String> usedForThorntail) {
        this.communityDependency = communityDependency;
        this.usedForThorntail = usedForThorntail;
    }

    public String toCsvLine() {
        return String.format("%s; %s", communityDependency.toCsvLine(), usedForThorntail);
    }

    public List<String> getUsedForThorntail() {
        return usedForThorntail;
    }

    @java.lang.SuppressWarnings("all")
    public CommunityDependency getCommunityDependency() {
        return this.communityDependency;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsedForThorntail(final List<String> usedForThorntail) {
        this.usedForThorntail = usedForThorntail;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ThorntailCommunityDependency(communityDependency=" + this.getCommunityDependency()
                + ", usedForThorntail=" + this.getUsedForThorntail() + ")";
    }
}
