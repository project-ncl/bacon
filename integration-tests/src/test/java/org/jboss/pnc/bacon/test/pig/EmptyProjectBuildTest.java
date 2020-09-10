package org.jboss.pnc.bacon.test.pig;

import org.jboss.pnc.bacon.pig.PigFacade;
import org.jboss.pnc.bacon.pig.impl.config.GroupBuildInfo;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildConfigData;
import org.jboss.pnc.bacon.pig.impl.pnc.ImportResult;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.enums.RebuildMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class EmptyProjectBuildTest extends PigFunctionalTest {

    @Test
    void shouldConfigureEmptyProject(@TempDir Path targetDir) {
        String suffix = init(Paths.get("src", "test", "resources", "empty"), true, Optional.empty(), targetDir);

        ImportResult importResult = PigFacade.configure(true, false);
        assertThat(importResult.getBuildGroup().getId()).isNotNull();

        assertThat(importResult.getBuildConfigs()).hasSize(2);
        assertThat(importResult.getBuildConfigs().stream().map(BuildConfigData::getName))
                .contains(format(emptyNameBase1, suffix), format(emptyNameBase2, suffix));
    }

    @Test
    void shouldBuildEmpty(@TempDir Path targetDir) {
        String suffix = init(Paths.get("src", "test", "resources", "empty"), true, Optional.empty(), targetDir);

        PigFacade.configure(true, false);
        GroupBuildInfo build = PigFacade.build(false, false, RebuildMode.EXPLICIT_DEPENDENCY_CHECK);
        assertThat(build.getBuilds()).isNotEmpty();

        assertThat(build.getBuilds().keySet()).contains(format(emptyNameBase1, suffix), format(emptyNameBase2, suffix));

        assertThat(build.getBuilds().values().stream()).allMatch(b -> b.getBuildStatus() == BuildStatus.SUCCESS);
    }

    @Test
    void shouldReturnPreviousBuildsOnRebuild(@TempDir Path targetDir) {
        String suffix = init(Paths.get("src", "test", "resources", "empty"), true, Optional.empty(), targetDir);

        PigFacade.configure(true, false);
        GroupBuildInfo build = PigFacade.build(false, false, RebuildMode.EXPLICIT_DEPENDENCY_CHECK);

        Map<String, PncBuild> builds = build.getBuilds();
        assertThat(builds).isNotEmpty();

        assertThat(builds.keySet()).contains(format(emptyNameBase1, suffix), format(emptyNameBase2, suffix));

        assertThat(builds.values().stream()).allMatch(b -> b.getBuildStatus() == BuildStatus.SUCCESS);
        List<String> successfulBuilds = buildIds(build);

        GroupBuildInfo rebuild = PigFacade.build(false, false, RebuildMode.EXPLICIT_DEPENDENCY_CHECK);
        assertThat(buildIds(rebuild)).containsAll(successfulBuilds);
    }

    private List<String> buildIds(GroupBuildInfo build) {
        Map<String, PncBuild> builds = build.getBuilds();
        return builds.values().stream().map(PncBuild::getId).collect(Collectors.toList());
    }
}
