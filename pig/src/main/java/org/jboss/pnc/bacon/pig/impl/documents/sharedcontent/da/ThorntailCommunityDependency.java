package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import groovy.lang.Delegate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
@Getter
@Setter
@ToString
public class ThorntailCommunityDependency implements CsvExportable {

    @Delegate
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
}
