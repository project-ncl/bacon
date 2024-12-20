package org.jboss.pnc.bacon.pig.impl.repo.visitor;

/**
 * Repository artifact visitor
 */
public interface ArtifactVisitor {

    /**
     * Called for each artifact present in a repository
     *
     * @param visit visited artifact
     */
    void visit(ArtifactVisit visit);
}
