package org.jboss.bacon.experimental.impl.dependencies;

import java.util.Comparator;

public class ProjectDepthComparator implements Comparator<Project> {
    @Override
    public int compare(Project p1, Project p2) {
        int r = p1.getDepth() - p2.getDepth();
        if (r == 0) {
            r = p1.getFirstGAV().compareTo(p2.getFirstGAV());
        }
        return r;
    }
}
