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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.config.Product;
import org.jboss.pnc.bacon.pig.impl.utils.CollectionUtils;
import org.jboss.pnc.bacon.pig.impl.utils.OSCommandException;
import org.jboss.pnc.bacon.pig.impl.utils.OSCommandExecutor;
import org.jboss.pnc.bacon.pig.impl.utils.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.jboss.pnc.bacon.pig.impl.pnc.PncConfigurator.getExistingMilestone;
import static org.jboss.pnc.bacon.pig.impl.pnc.PncDao.invokeAndParseListRetryingWithTimeout;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/28/17
 */
public class PncEntitiesImporter {
    private static final Logger log = LoggerFactory.getLogger(PncEntitiesImporter.class);
    public static final String LIST_PRODUCTS = "list-products";

    private int productId;
    private int versionId;
    private int milestoneId;
    private int buildGroupId;
    private List<BuildConfigData> configs;
    private Config config = PigContext.get().getConfig();

    public ImportResult performImport() {
        productId = getOrGenerateProduct();
        versionId = getOrGenerateVersion(productId);
        milestoneId = PncConfigurator.getOrGenerateMilestone(
                versionId,
                config.getMajorMinor(),
                pncMilestoneString(),
                config.getProduct().getIssueTrackerUrl()
        );
        PncConfigurator.markMilestoneCurrent(versionId, milestoneId);
        buildGroupId = getOrGenerateBuildGroup();

        configs = getAddOrUpdateBuildConfigs();
        log.debug("Setting up build dependencies");
        setUpBuildDependencies();

        log.debug("Adding builds to group");
        addBuildConfigIdsToGroup();
        return new ImportResult(milestoneId, buildGroupId, versionId, configs);
    }

    private void setUpBuildDependencies() {
        configs.parallelStream().forEach(this::setUpBuildDependencies);
    }

    private void setUpBuildDependencies(BuildConfigData config) {
        Integer id = config.getId();

        Set<Integer> dependencies =
                config.getDependencies()
                        .stream()
                        .map(this::getConfigIdByName)
                        .collect(Collectors.toSet());
        Set<Integer> currentDependencies = PncDao.invokeAndGetResultIds("list-dependencies -i " + id, 4);

        Set<Integer> superfluous = CollectionUtils.subtractSet(currentDependencies, dependencies);
        if (!superfluous.isEmpty()) {
            superfluous.forEach(
                    dependencyId -> PncDao.invoke(format("remove-dependency -i %d --dependency-id %d", id, dependencyId))
            );
        }

        Set<Integer> missing = CollectionUtils.subtractSet(dependencies, currentDependencies);
        if (!missing.isEmpty()) {
            missing.forEach(
                    dependencyId ->
                            PncDao.invoke(format("add-dependency -i %d --dependency-id %d", id, dependencyId))
            );
        }

        if (!superfluous.isEmpty() || !missing.isEmpty()) {
            config.setModified(true);
        }
    }

    private Integer getConfigIdByName(String name) {
        Optional<BuildConfigData> maybeConfig = configs.stream()
                .filter(c -> c.getName().equals(name))
                .findAny();
        return maybeConfig
                .orElseThrow(() -> new RuntimeException("Build config name " + name + " used to reference a dependency but no such build config defined"))
                .getId();
    }


    private void addBuildConfigIdsToGroup() {
        String configIdsAsString =
                configs.stream()
                        .map(BuildConfigData::getId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(" "));
        PncDao.invoke(
                format("update-build-configuration-set %d --build-configuration-ids %s", buildGroupId, configIdsAsString)
        );
    }

    private List<BuildConfigData> getAddOrUpdateBuildConfigs() {
        log.info("Adding/updating build configurations");
        List<PncBuildConfig> currentConfigs = getCurrentBuildConfigs(buildGroupId);
        dropConfigsFromInvalidVersion(currentConfigs, config.getBuilds(), versionId);
        return updateOrCreate(currentConfigs, config.getBuilds());
    }

    private PncBuildConfig getBuildConfig(String name) {
        List<String> output = PncDao.invoke(format("get-build-configuration -n %s", name), 4);
        return PncCliParser.parse(output, new TypeReference<PncBuildConfig>(){});
    }

    private List<BuildConfigData> updateOrCreate(
            List<PncBuildConfig> currentConfigs,
            List<BuildConfig> builds) {
        List<BuildConfigData> buildList = new ArrayList<>();
        for(BuildConfig bc : builds)
        {
            BuildConfigData data = new BuildConfigData(bc);
            for(PncBuildConfig config: currentConfigs)
            {
                if(config.getName().equals(bc.getName()))
                {
                    data.setOldConfig(config);
                    if(data.shouldBeUpdated()) {
                        updateBuildConfig(data);
                    }
                }
            }
            //Check if build exists already (globally)
            //True = Add to BCS and update BC (maybe ask?)
            PncBuildConfig matchedBuildConfig = null;
            try {
                matchedBuildConfig = getBuildConfig(bc.getName());
            } catch (OSCommandException e) {
                log.debug("No matching build config found in the BCS");
            }
            if (matchedBuildConfig != null) {
                log.debug("Found matching build config for {}", bc.getName());
                data.setOldConfig(matchedBuildConfig);
                if(data.shouldBeUpdated()) {
                    updateBuildConfig(data);
                }
                data.setModified(true);
            }
            else {
                //False = Create new project/BC
                Integer configId = createBuildConfig(data.getNewConfig());
                data.setId(configId);
                data.setModified(true);
                log.debug("Didn't find matching build config for {}", bc.getName());
            }
            buildList.add(data);
        }
        return buildList;
    }

    private Integer createBuildConfig(BuildConfig buildConfig) {
        Integer projectId = getOrGenerateProject(buildConfig.getProject());

        if (repositoryConfigExists(buildConfig) || buildConfig.getScmUrl() != null) {
            Integer repoId = getOrGenerateRepository(buildConfig);
            String createParams = buildConfig.toCreateParams(projectId, repoId, versionId);
            return PncDao.invokeAndGetResultId(format("create-build-configuration %s", createParams));
        }

        return createBuildConfigFromExternalUrl(projectId, buildConfig);
    }

    private Integer createBuildConfigFromExternalUrl(Integer projectId, BuildConfig buildConfig) {
        String createParams = buildConfig.toCreateParamsForExternalScm(projectId, versionId);
        // TODO: simplify when NCL-3866 is fixed
        String command = String.format("pnc create-build-configuration-process %s", createParams);
        List<String> output = OSCommandExecutor.executor(command)
                .redirectErrorStream(false)
                .exec()
                .getOut();
        log.debug("configuration creation output: {}", StringUtils.join(output, "\n"));
        log.debug("Due to NCL-3866, there's no way to tell if it succeeded, will wait for the configuration to be created");
        return SleepUtils.waitFor(
                () -> PncDao.invokeAndGetResultId("get-build-configuration -n " + buildConfig.getName(), 4),
                10,
                10 * 60,
                true,
                String.format("Timed out while waiting for build configuration %s to be created", buildConfig.getName())
        );

    }

    private boolean repositoryConfigExists(BuildConfig buildConfig) {
        return invokeAndParseListRetryingWithTimeout(
                "search-repository-configuration " + buildConfig.getShortScmURIPath(),
                60,
                4
        ).stream().anyMatch(buildConfig::matchesRepository);

    }


    private Integer getOrGenerateRepository(BuildConfig buildConfig) {
        return getOrGenerate(
                "search-repository-configuration " + buildConfig.getShortScmURIPath(),
                buildConfig::matchesRepository,
                () -> createRepository(buildConfig)
        );
    }

    private Integer createRepository(BuildConfig buildConfig) {
        // TODO validate that only one repo url is provided
        String command;
        if (buildConfig.getScmUrl() != null) {
            command = "create-repository-configuration --no-sync " + buildConfig.getScmUrl();
        } else if (buildConfig.getExternalScmUrl() != null) {
            throw new RuntimeException("create-repository-configuration should not be used for external scm urls!");
        } else {
            throw new RuntimeException("No scm url provided for config " + buildConfig);
        }
        return PncDao.invokeAndGetResultId(command);
    }

    private Integer updateBuildConfig(BuildConfigData data) {
        Integer configId = data.getId();
        Integer projectId = getOrGenerateProject(data.getProject());
        String updateParams = data.getNewConfig().toUpdateParams(projectId, data.getOldConfig());
        log.info("Updating build configuration {}", data.getName());
        PncDao.invoke(format("update-build-configuration %d %s", configId, updateParams));
        return configId;
    }

    private Integer getOrGenerateProject(String projectName) {
        return getOrGenerate("list-projects -q name==" + projectName,
                any -> true,
                () -> generateProject(projectName)
        );
    }

    private Integer generateProject(String projectName) {
        String command = format("create-project \"%s\"", projectName);
        return PncDao.invokeAndGetResultId(command);
    }

    private List<PncBuildConfig> dropConfigsFromInvalidVersion(
            List<PncBuildConfig> currentConfigs,
            List<BuildConfig> newConfigs,
            int versionId) {
        Map<String, BuildConfig> newConfigsByName = BuildConfig.mapByName(newConfigs);
        List<PncBuildConfig> configsToDrop = currentConfigs.stream()
                .filter(config -> shouldBeDropped(config, versionId, newConfigsByName))
                .collect(Collectors.toList());
        if (!configsToDrop.isEmpty()) {
            throw new RuntimeException("The following configurations should be dropped or updated " +
                    "in an unsupported fashion, please drop or update them via PNC UI: " + configsToDrop +
                    ". Look above for the cause");
        }
        return configsToDrop;
    }

    private boolean shouldBeDropped(PncBuildConfig oldConfig,
                                    int versionId,
                                    Map<String, BuildConfig> newConfigsByName) {
        String name = oldConfig.getName();
        BuildConfig newConfig = newConfigsByName.get(name);
        boolean configMismatch = oldConfig.getVersionId() == null || oldConfig.getVersionId() != versionId;
        if (configMismatch) {
            log.warn("Product version in the old config is different than the one in the new config for config {}", name);
        }
        return configMismatch || newConfig == null || !oldConfig.isUpgradableTo(newConfig);
    }


    private List<PncBuildConfig> getCurrentBuildConfigs(int buildGroupId) {
        String command = format("list-build-configurations-for-set -i %d", buildGroupId);
        List<String> output = PncDao.invoke(command, 4);
        return PncCliParser.parseList(output, new TypeReference<List<PncBuildConfig>>() {
        });
    }

    private int getOrGenerateBuildGroup() {
        Optional<Integer> buildConfigSetId = getBuildGroup();
        return buildConfigSetId.orElseGet(() -> generateBuildGroup(versionId));
    }

    private Optional<Integer> getBuildGroup() {
        Optional<Integer> buildConfigSetId;
        try {
            buildConfigSetId = Optional.of(
                    PncDao.invokeAndGetResultId(format("get-build-configuration-set --name \"%s\"", config.getGroup()), 4)
            );
        } catch (OSCommandException e) {
            log.info(format("Product build group does not exist: {}, we'll create one", config.getGroup(), e));
            buildConfigSetId = Optional.empty();
        }
        return buildConfigSetId;
    }

    private int getOrGenerateVersion(int productId) {
        String command = versionsForProduct(productId);
        return getOrGenerate(
                command,
                versionByMajorMinor(),
                () -> generateVersion(productId)
        );
    }

    private String versionsForProduct(int productId) {
        return format("list-versions-for-product -i %d", productId);
    }

    private Predicate<Map<String, ?>> versionByMajorMinor() {
        String version = config.getMajorMinor();
        return entry -> entry.get("version").equals(version);
    }

    private int getOrGenerateProduct() {
        return getOrGenerate(
                LIST_PRODUCTS,
                productByName(),
                this::generateProduct
        );
    }

    private Predicate<Map<String, ?>> productByName() {
        String productName = config.getProduct().getName();
        return product -> product.get("name").equals(productName);
    }

    private Optional<Integer> get(String listCommand, Predicate<Map<String, ?>> existenceCheck) {
        List<Map<String, ?>> items = invokeAndParseListRetryingWithTimeout(listCommand, 60, 4);

        return items.stream()
                .filter(existenceCheck)
                .findAny()
                .map(m -> (Integer) m.get("id"));
    }

    private Integer getOrGenerate(String listCommand,
                                  Predicate<Map<String, ?>> existenceCheck,
                                  Supplier<Integer> generator) {
        return get(listCommand, existenceCheck)
                .orElseGet(generator);
    }

    private Integer generateVersion(Integer productId) {
        String version = config.getMajorMinor();
        String command = format("create-product-version %d \"%s\"", productId, version);
        return PncDao.invokeAndGetResultId(command);
    }

    private Integer generateProduct() {
        Product product = config.getProduct();
        String command =
                format("create-product \"%s\" \"%s\"", product.getName(), product.getAbbreviation());
        return PncDao.invokeAndGetResultId(command);
    }


    private Integer generateBuildGroup(Integer versionId) {
        String group = config.getGroup();
        String command = format("create-build-configuration-set -pvi %d \"%s\"", versionId, group);
        return PncDao.invokeAndGetResultId(command);
    }

    public ImportResult readCurrentPncEntities() {
        productId = getProduct();
        versionId = getVersion();
        Optional<Integer> maybeMilestone = getExistingMilestone(versionId, config.getMajorMinor(), pncMilestoneString());
        milestoneId = maybeMilestone
                .orElseThrow(() -> new RuntimeException("Unable to find milestone " + pncMilestoneString())); // TODO

        buildGroupId = getBuildGroup()
                .orElseThrow(() -> new RuntimeException("Unable to find build group " + config.getGroup()));

        configs = getBuildConfigs();

        return new ImportResult(milestoneId, buildGroupId, versionId, configs);
    }

    private List<BuildConfigData> getBuildConfigs() {
        List<PncBuildConfig> configs = getCurrentBuildConfigs(buildGroupId);

        return configs.stream()
                .map(config -> {
                    BuildConfigData result = new BuildConfigData(null);
                    result.setOldConfig(config);
                    return result;
                }).collect(Collectors.toList());
    }

    private int getVersion() {
        return get(versionsForProduct(productId), versionByMajorMinor())
                .orElseThrow(() -> new RuntimeException("Unable to find version " + config.getMajorMinor() + " for product " + productId));
    }

    private Integer getProduct() {
        Optional<Integer> maybeProductId = get(LIST_PRODUCTS, productByName());
        return maybeProductId
                .orElseThrow(() -> new RuntimeException("Unable to find product called " + config.getProduct().getName()));
    }

    private String pncMilestoneString() {
        return config.getMicro() + "." + config.getMilestone();
    }
}
