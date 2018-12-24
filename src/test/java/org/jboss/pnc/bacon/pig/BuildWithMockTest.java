package org.jboss.pnc.bacon.pig;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableWeld
class BuildWithMockTest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(Build.class, BuildWithMockTest.class);

    @Inject
    private Build build;

    @ApplicationScoped
    @Produces
    BuildDependency produceBuildDependency() {
        return when(mock(BuildDependency.class).doSomething(anyString())).thenReturn("dummy")
                .getMock();
    }

    @Test
    void assertExecute() {
        assertThat(build.execute(new Build.Input("/tmp", -1))).contains("dummy");
    }
}