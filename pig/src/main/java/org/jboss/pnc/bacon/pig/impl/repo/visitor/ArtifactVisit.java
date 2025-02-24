package org.jboss.pnc.bacon.pig.impl.repo.visitor;

import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;

/**
 * Information about a visited artifact
 */
public interface ArtifactVisit {

    GAV getGav();

    Map<String, String> getChecksums();
}
