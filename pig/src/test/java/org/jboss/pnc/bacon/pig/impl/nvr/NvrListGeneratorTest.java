/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.nvr;

import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.BuildFinderUtils;
import org.jboss.pnc.build.finder.core.BuildFinderObjectMapper;
import org.jboss.pnc.build.finder.koji.KojiBuild;
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
            URL url = NvrListGenerator.class.getClassLoader().getResource("builds.json");

            assertThat(url).isNotNull();

            File buildsJson = Paths.get(url.toURI()).toFile();
            List<KojiBuild> builds = Collections.unmodifiableList(
                    Arrays.asList(new BuildFinderObjectMapper().readValue(buildsJson, KojiBuild[].class)));
            Map<String, Collection<String>> checksums = Collections.emptyMap();

            utilsMockedStatic.when(() -> BuildFinderUtils.findBuilds(checksums, true)).thenReturn(builds);

            assertThat(NvrListGenerator.generateNvrList(checksums, target)).isTrue();

            String nvrTxt = FileUtils.readFileToString(target.toFile(), StandardCharsets.UTF_8);

            assertThat(nvrTxt.trim()).isEqualTo("org.apache.commons-commons-lang3-3.6.0.redhat_1-1");
        }
    }
}
