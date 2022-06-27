package org.jboss.pnc.bacon.test.da;

import org.jboss.pnc.bacon.test.CLIExecutor;
import org.jboss.pnc.bacon.test.ExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DAMavenLatestTest {

    @Test
    void testMavenLatestGAVsSpecified() {
        ExecutionResult result = CLIExecutor.runCommand("da", "lookup", "maven-latest");
        assertThat(result.getError()).contains("You didn't specify any GAVs or file!");
        assertThat(result.getRetval()).isNotZero();
    }
}
