package org.jboss.pnc.bacon.test.pig;

import org.jboss.pnc.bacon.test.AbstractTest;
import org.jboss.pnc.bacon.test.CLIExecutor;
import org.jboss.pnc.bacon.test.Endpoints;
import org.jboss.pnc.bacon.test.ExecutionResult;
import org.jboss.pnc.bacon.test.PNCWiremockHelper;
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
        final Path configFile = CLIExecutor.CONFIG_LOCATION;
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

        PNCWiremockHelper.list(Endpoints.PRODUCT, new Page<Product>());
        PNCWiremockHelper.creation(Endpoints.PRODUCT, product);
        PNCWiremockHelper.list(Endpoints.PRODUCT_VERSIONS.apply(UNIVERSAL_ID), new Page<ProductVersion>());
        PNCWiremockHelper.creation(Endpoints.PRODUCT_VERSION, productVersion);
        PNCWiremockHelper.list(Endpoints.PRODUCT_VERSION_MILESTONES.apply(UNIVERSAL_ID), new Page<ProductMilestone>());
        PNCWiremockHelper.creation(Endpoints.PRODUCT_MILESTONE, productMilestone);
        PNCWiremockHelper.update(Endpoints.PRODUCT_VERSION, productVersion, productVersionWithCurrentMilestone);
        PNCWiremockHelper.list(Endpoints.GROUP_CONFIG, new Page<GroupConfiguration>());
        PNCWiremockHelper.creation(Endpoints.GROUP_CONFIG, groupConfig);
        PNCWiremockHelper
                .list(Endpoints.GROUP_CONFIG_BUILD_CONFIGS.apply(UNIVERSAL_ID), new Page<BuildConfiguration>());
        PNCWiremockHelper.list(Endpoints.BUILD_CONFIG, new Page<BuildConfiguration>());
        PNCWiremockHelper.list(Endpoints.PROJECT, new Page<Project>());
        PNCWiremockHelper.creation(Endpoints.PROJECT, project);
        PNCWiremockHelper.list(Endpoints.SCM_REPOSITORY, new Page<SCMRepository>());
        PNCWiremockHelper.creation(
                Endpoints.SCM_REPOSITORY_CREATE,
                RepositoryCreationResponse.builder().repository(scmRepository).build());
        PNCWiremockHelper.creation(Endpoints.BUILD_CONFIG, buildConfig);
        PNCWiremockHelper.list(Endpoints.BUILD_CONFIG_DEPENDENCIES.apply(UNIVERSAL_ID), new Page<BuildConfiguration>());
        PNCWiremockHelper.get(Endpoints.BUILD_CONFIG, buildConfig);
        PNCWiremockHelper.creation(Endpoints.BUILD_CONFIG, buildConfig);

        PNCWiremockHelper.scenario("add BC to GC")
                .getEntity(Endpoints.GROUP_CONFIG, groupConfig)
                .when()
                .post(Endpoints.GROUP_CONFIG_BUILD_CONFIGS.apply(UNIVERSAL_ID))
                .then()
                .getEntity(Endpoints.GROUP_CONFIG, groupConfigWithBuildConfig);
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
