package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;

public class BomMultiStepRepositoryStrategyTest extends MultiStepBomBasedRepositoryTestBase {

    @Override
    protected void assertOutcome() {
        assertCachi2LockFile(CACHI2_LOCKFILE_NAME);
    }

    @Override
    protected RepoGenerationStrategy getRepoGenerationStrategy() {
        return RepoGenerationStrategy.BOM;
    }
}
