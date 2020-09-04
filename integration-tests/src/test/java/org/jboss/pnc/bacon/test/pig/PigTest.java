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
package org.jboss.pnc.bacon.test.pig;

import org.jboss.pnc.bacon.test.AbstractTest;
import org.jboss.pnc.bacon.test.ExecutionResult;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.Product;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.Project;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.response.Page;
import org.jboss.pnc.dto.response.RepositoryCreationResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.bacon.test.CLIExecutor.CONFIG_LOCATION;
import static org.jboss.pnc.bacon.test.Endpoints.BUILD_CONFIG;
import static org.jboss.pnc.bacon.test.Endpoints.BUILD_CONFIG_DEPENDENCIES;
import static org.jboss.pnc.bacon.test.Endpoints.GROUP_CONFIG;
import static org.jboss.pnc.bacon.test.Endpoints.GROUP_CONFIG_BUILD_CONFIGS;
import static org.jboss.pnc.bacon.test.Endpoints.PRODUCT;
import static org.jboss.pnc.bacon.test.Endpoints.PRODUCT_MILESTONE;
import static org.jboss.pnc.bacon.test.Endpoints.PRODUCT_VERSION;
import static org.jboss.pnc.bacon.test.Endpoints.PRODUCT_VERSIONS;
import static org.jboss.pnc.bacon.test.Endpoints.PRODUCT_VERSION_MILESTONES;
import static org.jboss.pnc.bacon.test.Endpoints.PROJECT;
import static org.jboss.pnc.bacon.test.Endpoints.SCM_REPOSITORY;
import static org.jboss.pnc.bacon.test.Endpoints.SCM_REPOSITORY_CREATE;

/**
 *
 * @author jbrazdil
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PigTest extends AbstractTest {

    private static final String UNIVERSAL_ID = "42";
    private static final String SUFFIX = getRandomString();
    private static final String PRODUCT_NAME = "Product Foobar " + SUFFIX;
    private static final String PRODUCT_ABBREVIATION = "PFB-" + SUFFIX;
    private static final String VERSION = "7.1.0";
    private static final String MILESTONE = VERSION + ".DR1";
    private static final String PROJECT_NAME = "my-project " + SUFFIX;
    private static final String BC_NAME = "my-app-a-7.1-" + SUFFIX;
    private static final String SCM_URL = "git+ssh://code.engineering.redhat.com/my-project/my-app-a.git";
    private static final String SCM_REVISION = "7.1.x";
    private static final String BUILD_SCRIPT = "mvn -Dswarm.product.build -DskipTests clean deploy -Plicenses";
    private static final String GROUP_NAME = "ProductA 7.1 all " + SUFFIX;

    @Test
    @Order(1)
    void shouldCreateProduct() throws IOException {
        final Path configFile = CONFIG_LOCATION;
        replaceSuffixInConfigFile(configFile.resolve("build-config.yaml"));

        final Product product = Product.builder()
                .id(UNIVERSAL_ID)
                .name(PRODUCT_NAME)
                .abbreviation(PRODUCT_ABBREVIATION)
                .build();
        final ProductVersion productVersion = ProductVersion.builder()
                .id(UNIVERSAL_ID)
                .version(VERSION)
                .product(product)
                .build();
        final ProductMilestone productMilestone = ProductMilestone.builder()
                .id(UNIVERSAL_ID)
                .productVersion(productVersion)
                .version(MILESTONE)
                .build();
        final ProductVersion productVersionWithCurrentMilestone = productVersion.toBuilder()
                .currentProductMilestone(productMilestone)
                .build();
        final GroupConfiguration groupConfig = GroupConfiguration.builder()
                .id(UNIVERSAL_ID)
                .name(GROUP_NAME)
                .productVersion(productVersionWithCurrentMilestone)
                .build();
        final Project project = Project.builder().id(UNIVERSAL_ID).name(PROJECT_NAME).build();
        final SCMRepository scmRepository = SCMRepository.builder()
                .id(UNIVERSAL_ID)
                .internalUrl(SCM_URL)
                .preBuildSyncEnabled(true)
                .build();
        final BuildConfiguration buildConfig = BuildConfiguration.builder()
                .id(UNIVERSAL_ID)
                .name(BC_NAME)
                .buildScript(BUILD_SCRIPT)
                .scmRevision(SCM_REVISION)
                .creationTime(Instant.now())
                .modificationTime(Instant.now())
                .scmRepository(scmRepository)
                .environment(Environment.builder().id(UNIVERSAL_ID).build())
                .project(project)
                .productVersion(productVersionWithCurrentMilestone)
                .build();
        final GroupConfiguration groupConfigWithBuildConfig = groupConfig.toBuilder()
                .buildConfigs(Collections.singletonMap(UNIVERSAL_ID, buildConfig))
                .build();

        wmock.list(PRODUCT, new Page<Product>());
        wmock.creation(PRODUCT, product);
        wmock.list(PRODUCT_VERSIONS.apply(UNIVERSAL_ID), new Page<ProductVersion>());
        wmock.creation(PRODUCT_VERSION, productVersion);
        wmock.list(PRODUCT_VERSION_MILESTONES.apply(UNIVERSAL_ID), new Page<ProductMilestone>());
        wmock.creation(PRODUCT_MILESTONE, productMilestone);
        wmock.update(PRODUCT_VERSION, productVersion, productVersionWithCurrentMilestone);
        wmock.list(GROUP_CONFIG, new Page<GroupConfiguration>());
        wmock.creation(GROUP_CONFIG, groupConfig);
        wmock.list(GROUP_CONFIG_BUILD_CONFIGS.apply(UNIVERSAL_ID), new Page<BuildConfiguration>());
        wmock.list(BUILD_CONFIG, new Page<BuildConfiguration>());
        wmock.list(PROJECT, new Page<Project>());
        wmock.creation(PROJECT, project);
        wmock.list(SCM_REPOSITORY, new Page<SCMRepository>());
        wmock.creation(SCM_REPOSITORY_CREATE, RepositoryCreationResponse.builder().repository(scmRepository).build());
        wmock.creation(BUILD_CONFIG, buildConfig);
        wmock.list(BUILD_CONFIG_DEPENDENCIES.apply(UNIVERSAL_ID), new Page<BuildConfiguration>());
        wmock.get(BUILD_CONFIG, buildConfig);
        wmock.creation(BUILD_CONFIG, buildConfig);

        wmock.scenario("add BC to GC")
                .getEntity(GROUP_CONFIG, groupConfig)
                .when()
                .post(GROUP_CONFIG_BUILD_CONFIGS.apply(UNIVERSAL_ID))
                .then()
                .getEntity(GROUP_CONFIG, groupConfigWithBuildConfig);
        ExecutionResult output = executeAndGetResult("pig", "configure", configFile.toString());
        assertThat(output.getOutput()).contains("name: \"Product Foobar " + SUFFIX + "\"");
    }

    private void replaceSuffixInConfigFile(Path configPath) throws IOException {
        try (Stream<String> stream = Files.lines(configPath)) {
            List<String> fileContent = stream.map(l -> l.contains("#!suffix=") ? "#!suffix=" + SUFFIX : l)
                    .collect(Collectors.toList());
            Files.write(configPath, fileContent);
        }
    }
}
