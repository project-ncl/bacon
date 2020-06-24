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

import org.jboss.pnc.bacon.common.futures.FutureUtils;
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
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.ProductClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.ProjectClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRef;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.Product;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductRef;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.Project;
import org.jboss.pnc.dto.ProjectRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.requests.CreateAndSyncSCMRequest;
import org.jboss.pnc.dto.response.RepositoryCreationResponse;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.restclient.AdvancedSCMRepositoryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
public class PncEntitiesImporter {
    private static final Logger log = LoggerFactory.getLogger(PncEntitiesImporter.class);

    private final BuildConfigurationClient buildConfigClient;
    private final GroupConfigurationClient groupConfigClient;
    private final ProductClient productClient;
    private final ProjectClient projectClient;
    private final AdvancedSCMRepositoryClient repoClient;
    private final ProductVersionClient versionClient;

    private ProductRef product;
    private ProductVersion version;
    private ProductMilestone milestone;
    private GroupConfiguration buildGroup;
    private List<BuildConfigData> configs;
    private final PigConfiguration pigConfiguration = PigContext.get().getPigConfiguration();

    private final PncConfigurator pncConfigurator = new PncConfigurator();

    public PncEntitiesImporter() {
        buildConfigClient = new BuildConfigurationClient(PncClientHelper.getPncConfiguration());
        groupConfigClient = new GroupConfigurationClient(PncClientHelper.getPncConfiguration());
        productClient = new ProductClient(PncClientHelper.getPncConfiguration());
        projectClient = new ProjectClient(PncClientHelper.getPncConfiguration());
        repoClient = new AdvancedSCMRepositoryClient(PncClientHelper.getPncConfiguration());
        versionClient = new ProductVersionClient(PncClientHelper.getPncConfiguration());
    }

    public ImportResult performImport(boolean skipBranchCheck) {
        product = getOrGenerateProduct();
        version = getOrGenerateVersion();
        milestone = pncConfigurator.getOrGenerateMilestone(version, pncMilestoneString());
        pncConfigurator.markMilestoneCurrent(version, milestone);
        buildGroup = getOrGenerateBuildGroup();

        configs = getAddOrUpdateBuildConfigs(skipBranchCheck);
        log.debug("Setting up build dependencies");
        setUpBuildDependencies();

        log.debug("Adding builds to group");
        addBuildConfigIdsToGroup();
        return new ImportResult(milestone, buildGroup, version, configs);
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

    private List<BuildConfigData> getAddOrUpdateBuildConfigs(boolean skipBranchCheck) {
        log.info("Adding/updating build configurations");
        List<BuildConfiguration> currentConfigs = getCurrentBuildConfigs();
        dropConfigsFromInvalidVersion(currentConfigs, pigConfiguration.getBuilds());
        return updateOrCreate(currentConfigs, pigConfiguration.getBuilds(), skipBranchCheck);
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
            boolean skipBranchCheck) {
        List<BuildConfigData> buildList = new ArrayList<>();
        for (BuildConfig bc : builds) {
            BuildConfigData data = new BuildConfigData(bc);
            for (BuildConfiguration config : currentConfigs) {
                if (config.getName().equals(bc.getName())) {
                    data.setOldConfig(config);
                    if (data.shouldBeUpdated(skipBranchCheck)) {
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
                if (data.shouldBeUpdated(skipBranchCheck)) {
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
                .build();
    }

    private SCMRepository getOrGenerateRepository(BuildConfig buildConfig) {
        Optional<SCMRepository> existingRepository = getExistingRepository(buildConfig);
        return existingRepository.orElseGet(() -> createRepository(buildConfig));
    }

    private Optional<SCMRepository> getExistingRepository(BuildConfig buildConfig) {

        String matchUrl = buildConfig.getScmUrl();

        if (matchUrl == null) {
            matchUrl = buildConfig.getExternalScmUrl();
        }

        try {
            return toStream(repoClient.getAll(matchUrl, null)).filter(buildConfig::matchesRepository).findAny();
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to search for repository by " + matchUrl, e);
        }
    }

    private SCMRepository createRepository(BuildConfig buildConfig) {
        String scmUrl = buildConfig.getScmUrl() == null ? buildConfig.getExternalScmUrl() : buildConfig.getScmUrl();
        CreateAndSyncSCMRequest createRepoRequest = CreateAndSyncSCMRequest.builder()
                .preBuildSyncEnabled(true)
                .scmUrl(scmUrl)
                .build();
        try {
            CompletableFuture<AdvancedSCMRepositoryClient.SCMCreationResult> response = repoClient
                    .createNewAndWait(createRepoRequest);

            log.info("Waiting for repository creation of '{}'", scmUrl);
            FutureUtils.printDotWhileFutureIsInProgress(response, 10);
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
        RemoteCollection<Project> query = null;
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
        List<BuildConfiguration> configsToDrop = currentConfigs.stream()
                .filter(config -> shouldBeDropped(config, newConfigsByName))
                .collect(Collectors.toList());
        if (!configsToDrop.isEmpty()) {
            throw new RuntimeException(
                    "The following configurations should be dropped or updated "
                            + "in an unsupported fashion, please drop or update them via PNC UI: " + configsToDrop
                            + ". Look above for the cause");
        }
        return configsToDrop;
    }

    private boolean shouldBeDropped(BuildConfiguration oldConfig, Map<String, BuildConfig> newConfigsByName) {
        String name = oldConfig.getName();
        BuildConfig newConfig = newConfigsByName.get(name);
        ProductVersionRef productVersion = oldConfig.getProductVersion();
        boolean configMismatch = productVersion == null || !productVersion.getId().equals(version.getId());
        if (configMismatch) {
            log.warn(
                    "Product version in the old config is different than the one in the new config for config {}",
                    name);
        }
        return configMismatch || newConfig == null || !newConfig.isUpgradableFrom(oldConfig);
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

    private Product generateProduct() {
        ProductConfig productConfig = pigConfiguration.getProduct();
        Product product = Product.builder()
                .name(productConfig.getName())
                .abbreviation(productConfig.getAbbreviation())
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
                () -> new RuntimeException("Unable to product " + pigConfiguration.getProduct().getName()));
        version = getVersion().orElseThrow(
                () -> new RuntimeException(
                        "Unable to find version " + pigConfiguration.getMajorMinor() + " for product " + product));
        milestone = pncConfigurator.getExistingMilestone(version, pncMilestoneString())
                .orElseThrow(() -> new RuntimeException("Unable to find milestone " + pncMilestoneString())); // TODO

        buildGroup = getBuildGroup()
                .orElseThrow(() -> new RuntimeException("Unable to find build group " + pigConfiguration.getGroup()));

        configs = getBuildConfigs();

        return new ImportResult(milestone, buildGroup, version, configs);
    }

    private List<BuildConfigData> getBuildConfigs() {
        List<BuildConfiguration> configs = getCurrentBuildConfigs();

        return configs.stream().map(config -> {
            BuildConfigData result = new BuildConfigData(null);
            result.setOldConfig(config);
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

    private String pncMilestoneString() {
        return pigConfiguration.getMajorMinor() + "." + pigConfiguration.getMicro() + "."
                + pigConfiguration.getMilestone();
    }
}
