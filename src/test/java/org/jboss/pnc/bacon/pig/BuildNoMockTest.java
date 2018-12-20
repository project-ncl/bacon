package org.jboss.pnc.bacon.pig;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@EnableWeld
class BuildNoMockTest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(Build.class, DummyBuildDependency.class);

    @Inject
    private Build build;

    @Test
    void assertExecute() {
        assertThat(build.execute(new Build.Input("/tmp", -1))).contains("/tmp");
    }
}