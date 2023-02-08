package org.jboss.bacon.experimental.impl.generator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildConfigGeneratorTest {

    @Test
    public void testOverrideRemoval() {
        String param = "test -DpluginOverride.*:*@*=  -DrepoReportingRemoval=true -DprojectMetaSkip=true -DdependencyOverride.javax.persistence:javax.persistence-api@*= -DprojectSrcSkip=false -DversionOverride=39";
        String paramNoDepOverride = BuildConfigGenerator.removeOverride(param, true);
        assertThat(paramNoDepOverride).isEqualTo(
                "test -DpluginOverride.*:*@*=  -DrepoReportingRemoval=true -DprojectMetaSkip=true -DprojectSrcSkip=false -DversionOverride=39");
        String paramNoOverride = BuildConfigGenerator.removeOverride(param, false);
        assertThat(paramNoOverride).isEqualTo(
                "test -DpluginOverride.*:*@*=  -DrepoReportingRemoval=true -DprojectMetaSkip=true -DprojectSrcSkip=false");
    }
}
