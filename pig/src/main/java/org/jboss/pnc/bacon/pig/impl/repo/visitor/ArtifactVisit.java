package org.jboss.pnc.bacon.pig.impl.repo.visitor;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.util.Map;

/**
 * Information about a visited artifact
 */
public interface ArtifactVisit {

    GAV getGav();

    Map<String, String> getChecksums();
}
