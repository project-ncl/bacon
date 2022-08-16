/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.pnc;

import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.ProductConfig;
import org.jboss.pnc.bacon.pig.impl.utils.CollectionUtils;
import org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils;
import org.jboss.pnc.bacon.pig.impl.utils.SleepUtils;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.EnvironmentClient;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.ProductClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.ProjectClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.constants.Attributes;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRef;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.Product;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductMilestoneRef;
import org.jboss.pnc.dto.ProductRef;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.Project;
import org.jboss.pnc.dto.ProjectRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.requests.CreateAndSyncSCMRequest;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.restclient.AdvancedSCMRepositoryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.findByNameQuery;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.maybeSingle;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.query;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.toStream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
public class PncEntitiesImporter implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PncEntitiesImporter.class);

    private final BuildConfigurationClient buildConfigClient;
    private final GroupConfigurationClient groupConfigClient;
    private final ProductClient productClient;
    private final ProjectClient projectClient;
    private final AdvancedSCMRepositoryClient repoClient;
    private final ProductVersionClient versionClient;
    private final EnvironmentClient environmentClient;

    private ProductRef product;
    private ProductVersion version;
    private ProductMilestone milestone;
    private GroupConfiguration buildGroup;
    private List<BuildConfigData> configs;
    private final PigConfiguration pigConfiguration = PigContext.get().getPigConfiguration();

    private final PncConfigurator pncConfigurator;

    public PncEntitiesImporter() {
        buildConfigClient = new BuildConfigurationClient(PncClientHelper.getPncConfiguration());
        groupConfigClient = new GroupConfigurationClient(PncClientHelper.getPncConfiguration());
        productClient = new ProductClient(PncClientHelper.getPncConfiguration());
        projectClient = new ProjectClient(PncClientHelper.getPncConfiguration());
        repoClient = new AdvancedSCMRepositoryClient(PncClientHelper.getPncConfiguration());
        versionClient = new ProductVersionClient(PncClientHelper.getPncConfiguration());
        environmentClient = new EnvironmentClient(PncClientHelper.getPncConfiguration());
        pncConfigurator = new PncConfigurator();
    }

    public ImportResult performImport(boolean skipBranchCheck, boolean temporaryBuild) {
        product = getOrGenerateProduct();
        version = getOrGenerateVersion();
        setBrewTagPrefix(version);

        milestone = pncConfigurator.getOrGenerateMilestone(version, PigContext.get().getFullVersion());
        pncConfigurator.markMilestoneCurrent(version, milestone);
        buildGroup = getOrGenerateBuildGroup();

        configs = getAddOrUpdateBuildConfigs(skipBranchCheck, temporaryBuild);
        checkForDeprecatedEnvironments(configs);
        log.debug("Setting up build dependencies");
        setUpBuildDependencies();

        log.debug("Adding builds to group");
        addBuildConfigIdsToGroup();
        return new ImportResult(milestone, buildGroup, version, configs);
    }

    public String getLatestProductMilestoneFullVersion() {
        try {
            Product product = maybeSingle(
                    productClient.getAll(empty(), findByNameQuery(this.pigConfiguration.getProduct().getName())))
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Error while retrieving current/latest Milestone. Product mentioned in build-config.yaml doesn't exist."));
            ProductVersion productVersion = maybeSingle(
                    productClient.getProductVersions(
                            product.getId(),
                            empty(),
                            query("version=='%s'", pigConfiguration.getMajorMinor()))).orElseThrow(
                                    () -> new RuntimeException(
                                            "Error while retrieving current/latest Milestone. Product Version mentioned in build-config.yaml doesn't exist for the Product."));

            ProductMilestoneRef currentProductMilestone = productVersion.getCurrentProductMilestone();
            if (currentProductMilestone == null) {
                throw new RuntimeException(
                        "Error while retrieving current/latest Milestone. No current milestone set for Product Version.");
            }
            return currentProductMilestone.getVersion();
        } catch (RemoteResourceException exception) {
            throw new RuntimeException(
                    "Error while retrieving current/latest Milestone. Reason: " + exception.getMessage());
        }
    }

    private void checkForDeprecatedEnvironments(List<BuildConfigData> configs) {
        log.debug("Checking for deprecated environments");
        Set<String> fetched = new HashSet<>();
        Set<String> deprecated = new HashSet<>();

        for (BuildConfigData config : configs) {
            String envId = config.getEnvironmentId();

            // skip if we already downloaded the environment
            if (fetched.contains(envId)) {
                if (deprecated.contains(envId)) {
                    log.warn(
                            "BuildConfig with the name: " + config.getName()
                                    + " is using deprecated environment. (NOTE: changing to updated environment may cause rebuilds)");
                }
                continue;
            }

            Environment env = getEnvironment(envId);
            fetched.add(envId);
            if (env.isDeprecated()) {
                log.warn(
                        "BuildConfig with the name: " + config.getName()
                                + " is using deprecated environment. (NOTE: changing to updated environment may cause rebuilds)");
                deprecated.add(envId);
            }
        }
    }

    private Environment getEnvironment(String envId) {
        try {
            return environmentClient.getSpecific(envId);
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to download the environment with id " + envId, e);
        }
    }

    private void setUpBuildDependencies() {
        configs.parallelStream().forEach(this::setUpBuildDependencies);
    }

    private void setUpBuildDependencies(BuildConfigData config) {
        String id = config.getId();

        // todo : store build configuration refs in BuildConfigData and use it instead of ids here
        Set<String> dependencies = config.getDependencies()
                .stream()
                .map(this::configByName)
                .collect(Collectors.toSet());
        Set<String> currentDependencies = getCurrentDependencies(id);

        Set<String> superfluous = CollectionUtils.subtractSet(currentDependencies, dependencies);
        if (!superfluous.isEmpty()) {
            superfluous.forEach(dependencyId -> removeDependency(id, dependencyId));
        }

        Set<String> missing = CollectionUtils.subtractSet(dependencies, currentDependencies);
        if (!missing.isEmpty()) {
            missing.stream().map(this::getBuildConfigFromId).forEach(dependency -> addDependency(id, dependency));
        }

        if (!superfluous.isEmpty() || !missing.isEmpty()) {
            config.setModified(true);
        }
    }

    private void addDependency(String configId, BuildConfiguration dependency) {
        try {
            buildConfigClient.addDependency(configId, dependency);
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to add dependency " + dependency.getId() + " to " + configId, e);
        }
    }

    private void removeDependency(String buildConfigId, String dependencyId) {
        try {
            buildConfigClient.removeDependency(buildConfigId, dependencyId);
        } catch (RemoteResourceException e) {
            throw new RuntimeException(
                    "Failed to remove dependency" + dependencyId + " from config" + buildConfigId,
                    e);
        }
    }

    private Set<String> getCurrentDependencies(String buildConfigId) {
        try {
            return toStream(buildConfigClient.getDependencies(buildConfigId)).map(BuildConfigurationRef::getId)
                    .collect(Collectors.toSet());
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get dependencies for build config " + buildConfigId, e);
        }
    }

    private String configByName(String name) {
        Optional<BuildConfigData> maybeConfig = configs.stream().filter(c -> c.getName().equals(name)).findAny();
        return maybeConfig
                .orElseThrow(
                        () -> new RuntimeException(
                                "Build config name " + name
                                        + " used to reference a dependency but no such build config defined"))
                .getId();
    }

    private void addBuildConfigIdsToGroup() {
        String configIdsAsString = configs.stream()
                .map(BuildConfigData::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        Set<String> existing = getExistingGroupConstituents();

        Set<String> target = configs.stream().map(BuildConfigData::getId).collect(Collectors.toSet());

        CollectionUtils.subtractSet(existing, target).forEach(this::removeConfigurationFromGroup);

        CollectionUtils.subtractSet(target, existing).forEach(this::addConfigurationToGroup);
    }

    private Set<String> getExistingGroupConstituents() {
        try {
            return toStream(groupConfigClient.getBuildConfigs(buildGroup.getId())).map(BuildConfiguration::getId)
                    .collect(Collectors.toSet());
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get configs from the group", e);
        }
    }

    private void removeConfigurationFromGroup(String superfluousId) {
        try {
            groupConfigClient.removeBuildConfig(buildGroup.getId(), superfluousId);
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to remove config " + superfluousId + " from the group", e);
        }
    }

    private void addConfigurationToGroup(String newConfigId) {
        try {
            groupConfigClient.addBuildConfig(buildGroup.getId(), getBuildConfigFromId(newConfigId));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to add config " + newConfigId + " to the group", e);
        }
    }

    private List<BuildConfigData> getAddOrUpdateBuildConfigs(boolean skipBranchCheck, boolean temporaryBuild) {
        log.info("Adding/updating build configurations");
        List<BuildConfiguration> currentConfigs = getCurrentBuildConfigs();
        dropConfigsFromInvalidVersion(currentConfigs, pigConfiguration.getBuilds());
        return updateOrCreate(currentConfigs, pigConfiguration.getBuilds(), skipBranchCheck, temporaryBuild);
    }

    private Optional<BuildConfiguration> getBuildConfigFromName(String name) {
        try {
            return maybeSingle(buildConfigClient.getAll(empty(), findByNameQuery(name)));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get build configuration " + name, e);
        }
    }

    private BuildConfiguration getBuildConfigFromId(String id) {
        try {
            return buildConfigClient.getSpecific(id);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to get build configuration " + id, e);
        }
    }

    private List<BuildConfigData> updateOrCreate(
            List<BuildConfiguration> currentConfigs,
            List<BuildConfig> builds,
            boolean skipBranchCheck,
            boolean temporaryBuild) {
        List<BuildConfigData> buildList = new ArrayList<>();
        for (BuildConfig bc : builds) {
            BuildConfigData data = new BuildConfigData(bc);
            for (BuildConfiguration config : currentConfigs) {
                if (config.getName().equals(bc.getName())) {
                    data.setOldConfig(config);
                    data.setId(config.getId());
                    if (data.shouldBeUpdated(skipBranchCheck, temporaryBuild)) {
                        updateBuildConfig(data, config);
                    }
                }
            }
            // Check if build exists already (globally)
            // True = Add to BCS and update BC (maybe ask?)
            Optional<BuildConfiguration> matchedBuildConfig = getBuildConfigFromName(bc.getName());
            if (matchedBuildConfig.isPresent()) {
                log.debug("Found matching build config for {}", bc.getName());
                data.setOldConfig(matchedBuildConfig.get());
                data.setId(matchedBuildConfig.get().getId());
                if (data.shouldBeUpdated(skipBranchCheck, temporaryBuild)) {
                    updateBuildConfig(data, matchedBuildConfig.get());
                }
                data.setModified(true);
            } else {
                log.debug("No matching build config found in the BCS");
                // False = Create new project/BC
                BuildConfiguration createdConfig = createBuildConfig(data.getNewConfig());
                data.setId(createdConfig.getId());
                data.setModified(true);
                log.debug("Didn't find matching build config for {}", bc.getName());
            }
            buildList.add(data);
        }
        return buildList;
    }

    private BuildConfiguration createBuildConfig(BuildConfig buildConfig) {
        BuildConfiguration config = generatePncBuildConfig(buildConfig);
        try {
            return buildConfigClient.createNew(config);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to create build configuration " + config, e);
        }
    }

    private BuildConfiguration generatePncBuildConfig(BuildConfig buildConfig) {
        return generatePncBuildConfig(buildConfig, null);
    }

    /**
     * @param buildConfig PiG buildconfig to generate
     * @param existing if present, we'll use it as the buildconfig to modify if null: we'll use a fresh buildconfig
     *        object
     *
     * @return BuildConfiguration generated
     */
    private BuildConfiguration generatePncBuildConfig(BuildConfig buildConfig, BuildConfiguration existing) {
        ProjectRef project = getOrGenerateProject(buildConfig.getProject());

        SCMRepository repository = getOrGenerateRepository(buildConfig);

        Environment environment = Environment.builder().id(buildConfig.getEnvironmentId()).build();

        BuildConfiguration.Builder builder = BuildConfiguration.builder();

        if (existing != null) {
            builder = existing.toBuilder();
        }

        return builder.productVersion(version)
                .parameters(buildConfig.getGenericParameters(null, false))
                .name(buildConfig.getName())
                .project(project)
                .environment(environment)
                .scmRepository(repository)
                .scmRevision(buildConfig.getScmRevision())
                .buildScript(buildConfig.getBuildScript())
                .buildType(BuildType.valueOf(buildConfig.getBuildType()))
                .brewPullActive(buildConfig.getBrewPullActive())
                .build();
    }

    private SCMRepository getOrGenerateRepository(BuildConfig buildConfig) {
        Optional<SCMRepository> existingRepository = getExistingRepository(buildConfig);
        return existingRepository.orElseGet(() -> createRepository(buildConfig));
    }

    private Optional<SCMRepository> getExistingRepository(BuildConfig buildConfig) {

        String matchUrl = buildConfig.getScmUrl();

        try {
            List<SCMRepository> foundRepository = toStream(repoClient.getAll(matchUrl, null))
                    .collect(Collectors.toList());
            if (foundRepository.isEmpty()) {
                return Optional.empty();
            } else if (foundRepository.size() == 1) {
                return Optional.of(foundRepository.get(0));
            } else {
                throw new RuntimeException(
                        "There exists more then one SCM Repository for url: " + matchUrl + " " + foundRepository);
            }
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to search for repository by " + matchUrl, e);
        }
    }

    private SCMRepository createRepository(BuildConfig buildConfig) {
        String scmUrl = buildConfig.getScmUrl();
        CreateAndSyncSCMRequest createRepoRequest = CreateAndSyncSCMRequest.builder()
                .preBuildSyncEnabled(true)
                .scmUrl(scmUrl)
                .build();
        try {
            CompletableFuture<AdvancedSCMRepositoryClient.SCMCreationResult> response = repoClient
                    .createNewAndWait(createRepoRequest);

            log.info("Waiting for repository creation of '{}'", scmUrl);
            SleepUtils.waitFor(response::isDone, 10, true);
            AdvancedSCMRepositoryClient.SCMCreationResult result = response.join();
            log.info("{}", result.toString());

            if (result.isSuccess()) {
                return result.getScmRepositoryCreationSuccess().getScmRepository();
            } else {
                throw new RuntimeException("Error on creation of repository: " + result.getRepositoryCreationFailure());
            }
        } catch (ClientException e) {
            throw new RuntimeException("Failed to trigger repository creation for " + scmUrl, e);
        }
    }

    private BuildConfiguration updateBuildConfig(BuildConfigData data, BuildConfiguration existing) {
        String configId = data.getId();

        BuildConfiguration buildConfiguration = generatePncBuildConfig(data.getNewConfig(), existing);

        try {
            buildConfigClient.update(configId, buildConfiguration);
            return buildConfigClient.getSpecific(configId);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to update build configuration " + configId, e);
        }
    }

    private Project getOrGenerateProject(String projectName) {
        RemoteCollection<Project> query;
        try {
            query = projectClient.getAll(empty(), findByNameQuery(projectName));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to search for project " + projectName, e);
        }
        return maybeSingle(query).orElseGet(() -> generateProject(projectName));
    }

    private Project generateProject(String projectName) {
        Project project = Project.builder().name(projectName).build();
        try {
            return projectClient.createNew(project);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to create project " + projectName, e);
        }
    }

    private List<BuildConfiguration> dropConfigsFromInvalidVersion(
            List<BuildConfiguration> currentConfigs,
            List<BuildConfig> newConfigs) {
        Map<String, BuildConfig> newConfigsByName = BuildConfig.mapByName(newConfigs);

        currentConfigs.stream().filter(config -> !newConfigsByName.containsKey(config.getName())).forEach(config -> {
            try {
                log.info("build config {} no longer defined, removing from build group", config.getId());
                groupConfigClient.removeBuildConfig(buildGroup.getId(), config.getId());
            } catch (RemoteResourceException e) {
                throw new RuntimeException(
                        "Failed to remove build config " + config.getId() + " from build group " + buildGroup.getId());
            }
        });

        List<BuildConfiguration> incompatibleConfigs = currentConfigs.stream()
                .filter(config -> newConfigsByName.containsKey(config.getName()))
                .filter(config -> isModifiedInUnsupportedWay(config, newConfigsByName))
                .collect(Collectors.toList());
        if (!incompatibleConfigs.isEmpty()) {
            throw new RuntimeException(
                    "The following configurations should be updated "
                            + "in an unsupported fashion, please drop or update them via PNC UI: " + incompatibleConfigs
                            + ". Look above for the cause");
        }
        return incompatibleConfigs;
    }

    private boolean isModifiedInUnsupportedWay(
            BuildConfiguration oldConfig,
            Map<String, BuildConfig> newConfigsByName) {
        String name = oldConfig.getName();
        BuildConfig newConfig = newConfigsByName.get(name);
        ProductVersionRef productVersion = oldConfig.getProductVersion();
        boolean configMismatch = productVersion == null || !productVersion.getId().equals(version.getId());
        if (configMismatch) {
            log.warn(
                    "Product version in the old config is different than the one in the new config for config {}",
                    name);
        }
        return configMismatch;
    }

    private List<BuildConfiguration> getCurrentBuildConfigs() {
        try {
            return PncClientUtils.toList(groupConfigClient.getBuildConfigs(buildGroup.getId()));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get configurations for config group: " + buildGroup.getId());
        }
    }

    private GroupConfiguration getOrGenerateBuildGroup() {
        Optional<GroupConfiguration> buildConfigSetId = getBuildGroup();
        return buildConfigSetId.orElseGet(() -> generateBuildGroup(version));
    }

    private Optional<GroupConfiguration> getBuildGroup() {
        try {
            return toStream(
                    groupConfigClient.getAll(empty(), Optional.of("name=='" + pigConfiguration.getGroup() + "'")))
                            .findAny();
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to check if build group exists");
        }
    }

    private ProductVersion getOrGenerateVersion() {
        return getVersion().orElseGet(this::generateVersion);
    }

    private Product getOrGenerateProduct() {
        return getProduct().orElseGet(this::generateProduct);
    }

    private Optional<Product> getProduct() {
        String productName = pigConfiguration.getProduct().getName();
        try {
            return maybeSingle(productClient.getAll(empty(), findByNameQuery(productName)));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get product data", e);
        }
    }

    private ProductVersion generateVersion() {
        ProductVersion productVersion = ProductVersion.builder()
                .product(product)
                .version(pigConfiguration.getMajorMinor())
                .build();
        try {
            return versionClient.createNew(productVersion);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to create the product version", e);
        }
    }

    /**
     * Override the default brewTagPrefix for the product version with the one specified in the pig configuration This
     * only happens if the brewTagPrefix in the pig configuration is not null/empty
     */
    private void setBrewTagPrefix(ProductVersion productVersion) {

        String brewTagPrefix = pigConfiguration.getBrewTagPrefix();

        if (brewTagPrefix != null && !brewTagPrefix.isEmpty()) {

            log.info("Updating the product version's brewTagPrefix with: {}", brewTagPrefix);

            Map<String, String> attributes = productVersion.getAttributes();
            attributes.put(Attributes.BREW_TAG_PREFIX, brewTagPrefix);

            ProductVersion update = productVersion.toBuilder().attributes(attributes).build();

            try {
                versionClient.update(productVersion.getId(), update);
            } catch (ClientException e) {
                throw new RuntimeException("Failed to update the brew tag prefix of the product version", e);
            }
        }
    }

    private Product generateProduct() {
        ProductConfig productConfig = pigConfiguration.getProduct();
        Product product = Product.builder()
                .name(productConfig.getName())
                .abbreviation(productConfig.getAbbreviation())
                .productManagers(productConfig.getProductManagers())
                .productPagesCode(productConfig.getProductPagesCode())
                .build();
        try {
            return productClient.createNew(product);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to create the product", e);
        }
    }

    private GroupConfiguration generateBuildGroup(ProductVersionRef version) {
        GroupConfiguration group = GroupConfiguration.builder()
                .productVersion(version)
                .name(pigConfiguration.getGroup())
                .build();
        try {
            return groupConfigClient.createNew(group);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to create group config: " + pigConfiguration.getGroup());
        }
    }

    public ImportResult readCurrentPncEntities() {
        product = getProduct().orElseThrow(
                () -> new RuntimeException("Unable to get product by name " + pigConfiguration.getProduct().getName()));
        version = getVersion().orElseThrow(
                () -> new RuntimeException(
                        "Unable to find version " + pigConfiguration.getMajorMinor() + " for product " + product));
        milestone = pncConfigurator.getExistingMilestone(version, PigContext.get().getFullVersion())
                .orElseThrow(
                        () -> new RuntimeException("Unable to find milestone " + PigContext.get().getFullVersion())); // TODO

        buildGroup = getBuildGroup()
                .orElseThrow(() -> new RuntimeException("Unable to find build group " + pigConfiguration.getGroup()));

        configs = getBuildConfigs();

        checkForDeprecatedEnvironments(configs);

        return new ImportResult(milestone, buildGroup, version, configs);
    }

    private List<BuildConfigData> getBuildConfigs() {
        List<BuildConfiguration> configs = getCurrentBuildConfigs();
        Map<String, BuildConfig> nameToBC = pigConfiguration.getBuilds()
                .stream()
                .collect(Collectors.toMap(BuildConfig::getName, bc -> bc));

        return configs.stream().map(config -> {
            BuildConfigData result = new BuildConfigData(nameToBC.get(config.getName()));
            result.setOldConfig(config);
            result.setId(config.getId());
            return result;
        }).collect(Collectors.toList());
    }

    private Optional<ProductVersion> getVersion() {
        try {
            Optional<String> byName = query("version=='%s'", pigConfiguration.getMajorMinor());
            return maybeSingle(productClient.getProductVersions(product.getId(), empty(), byName));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to query for version", e);
        }
    }

    @Override
    public void close() {
        buildConfigClient.close();
        groupConfigClient.close();
        productClient.close();
        projectClient.close();
        repoClient.close();
        versionClient.close();
        environmentClient.close();
        pncConfigurator.close();
    }
}
