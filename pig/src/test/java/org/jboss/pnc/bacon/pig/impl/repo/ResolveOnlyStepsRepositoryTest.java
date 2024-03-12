package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;

public class ResolveOnlyStepsRepositoryTest extends MultiStepBomBasedRepositoryTestBase {

    @Override
    protected RepoGenerationStrategy getRepoGenerationStrategy() {
        return RepoGenerationStrategy.RESOLVE_ONLY;
    }
}
