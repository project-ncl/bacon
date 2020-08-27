package org.jboss.pnc.bacon.pig.impl.nvr;

import com.redhat.red.build.finder.BuildFinderObjectMapper;
import com.redhat.red.build.finder.KojiBuild;
import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.BuildFinderUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NvrListGeneratorTest {
    @Test
    void shouldGenerateNvrList(@TempDir Path tempDir) throws URISyntaxException, IOException {
        try (MockedStatic<BuildFinderUtils> utilsMockedStatic = Mockito.mockStatic(BuildFinderUtils.class)) {
            String filename = "cpaas-1.0.0.ER1-nvr-list.txt";
            Path target = tempDir.resolve(filename).toAbsolutePath();
            String targetPath = target.toString();
            URL url = NvrListGenerator.class.getClassLoader().getResource("builds.json");

            assertThat(url).isNotNull();

            File buildsJson = Paths.get(url.toURI()).toFile();
            List<KojiBuild> builds = Collections.unmodifiableList(
                    Arrays.asList(new BuildFinderObjectMapper().readValue(buildsJson, KojiBuild[].class)));
            String repoZipPath = "cpaas-1.0.0.ER1-maven-repository.zip";
            Map<String, Collection<String>> checksums = Collections.emptyMap();

            utilsMockedStatic.when(() -> BuildFinderUtils.findBuilds(checksums, true)).thenReturn(builds);

            assertThat(NvrListGenerator.generateNvrList(checksums, targetPath)).isTrue();

            String nvrTxt = FileUtils.readFileToString(target.toFile(), StandardCharsets.UTF_8);

            assertThat(nvrTxt.trim()).isEqualTo("org.apache.commons-commons-lang3-3.6.0.redhat_1-1");
        }
    }
}
