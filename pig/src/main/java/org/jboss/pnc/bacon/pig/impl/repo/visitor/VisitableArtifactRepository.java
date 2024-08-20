package org.jboss.pnc.bacon.pig.impl.repo.visitor;

import java.nio.file.Path;

/**
 * Visitable Maven artifact repository
 */
public interface VisitableArtifactRepository {

    static VisitableArtifactRepository of(Path mavenRepo) {
        return new FileSystemArtifactRepository(mavenRepo);
    }

    /**
     * Visits artifacts present in the repository
     *
     * @param visitor artifact visitor
     */
    void visit(ArtifactVisitor visitor);

    /**
     * Total number of artifacts found in this repository.
     *
     * @return total number of artifacts found in this repository
     */
    int getArtifactsTotal();
}
