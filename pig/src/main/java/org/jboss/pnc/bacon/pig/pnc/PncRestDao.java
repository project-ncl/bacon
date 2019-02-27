/**
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
package org.jboss.pnc.bacon.pig.pnc;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.pnc.bacon.common.exception.TodoException;
import org.jboss.pnc.bacon.pig.PigException;
import org.jboss.pnc.bacon.pig.config.build.BuildConfig;
import org.jboss.pnc.bacon.pig.config.build.Product;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.GroupConfigurationClient;
import org.jboss.pnc.client.ProductClient;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.ProjectClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.SCMRepositoryClient;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRef;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductRef;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.Project;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.requests.CreateAndSyncSCMRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * for playing with the api to discover the options use: http://orch.cloud.pnc.engineering.redhat.com/pnc-web/apidocs/
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/5/18
 */
public class PncRestDao {
    private static final LocalDateTime START_DATE = LocalDateTime.now();
    private static final LocalDateTime END_DATE = LocalDateTime.now().plusDays(1);
    // TODO replace endpoint usage with Matej's client
    private BuildConfigurationClient configEndpoint;
    private GroupConfigurationClient configGroupEndpoint;
    private ProductClient productEndpoint;
    private ProjectClient projectEndpoint;
    private ProductVersionClient versionEndpoint;
    private ProductMilestoneClient milestoneEndpoint;
    private SCMRepositoryClient repositoryEndpoint;


    public Optional<BuildConfiguration> getBuildConfig(Integer id) {
        return failOnError(() -> configEndpoint.getSpecific(id));
    }

    public Collection<BuildConfiguration> listBuildConfigsInGroup(Integer groupId) {
        return failOnError(() ->
                toCollection(configGroupEndpoint.getConfigurations(groupId))
        );
    }

    @SuppressWarnings("rawtypes")
    public void markMilestoneCurrent(Integer versionId, Integer milestoneId) {
//        failOnError(() -> {
//            Optional<ProductMilestone> specific = milestoneEndpoint.getSpecific(milestoneId);
//            specific.ifPresent(
//                    milestone -> {
//                        milestoneEndpoint.update(milestoneId);
//                    }
//
//            );
//        }
    }

    protected Map<?, ?> getProductVersionAsMap(Integer versionId) {
        // todo
        throw new TodoException();
    }

    public Optional<ProductMilestone> getMilestoneIdForVersionAndName(Integer versionId, String milestoneName) {
//        failOnError(() -> {
//            List<Map> result = versionEndpoint.getMilestones(versionId)
//                    .forEach()
//            milestonesForVersion(versionId),
//                    Map.class,
//                    pair("q", "version==" + milestoneName));
//
//            return result.size() > 0
//                    ? Optional.of((Integer) result.iterator().next().get("id"))
//                    : Optional.empty();
//        });
        throw new TodoException();
    }

    public ProductMilestone createMilestone(Integer versionId, String milestoneName, String issueTrackerUrl) {
        ProductMilestone milestone = ProductMilestone.builder()
                .productVersion(ProductVersionRef.refBuilder().id(versionId).build())
                .version(milestoneName)
                .issueTrackerUrl(issueTrackerUrl)
                .startingDate(Instant.from(START_DATE)) // TODO: is it proper?
                .endDate(Instant.from(END_DATE))
                .build();
        return failOnError(() -> milestoneEndpoint.createNew(milestone));
    }

    public Optional<org.jboss.pnc.dto.Product> getProductByName(String name) {
        //todo
        throw new TodoException();
//        return getOptionalMatch(urls.products(), "name==" + name, PncProduct.class);
    }

    public Optional<ProductVersion> getProductVersion(int productId, String majorMinor) {
        return failOnError(() ->
                optional(productEndpoint.getProductVersions(productId, Collections.singletonMap("q", "version==" + majorMinor)))
        );
    }

    public Optional<Project> getProjectByName(String name) {
        return failOnError(() ->
                optional(projectEndpoint.getAll(Collections.singletonMap("q", "name==" + name)))
        );
    }

    private <T> Optional<T> optional(RemoteCollection<T> collection) {

        switch (collection.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(collection.iterator().next());
            default:
                throw new RuntimeException("Expected at most one match, got " + collection.size());
        }
    }

    public Optional<SCMRepository> getRepositoryConfigurationByScmUri(String shortScmURIPath) {
        return null;  // TODO: Customise this generated block
    }

    private NameValuePair pair(String key, String value) {
        return new BasicNameValuePair(key, value);
    }

    public Optional<SCMRepository> getRepositoryConfigurationForBuildConfigByScmUri(String shortScmURIPath) {
        return null;  // TODO: Customise this generated block
    }

    public org.jboss.pnc.dto.Product createProduct(Product product) {
        org.jboss.pnc.dto.Product pncProduct = org.jboss.pnc.dto.Product.builder()
                .abbreviation(product.getAbbreviation())
                .name(product.getName())
                .build();
        return failOnError(() -> productEndpoint.createNew(pncProduct));
    }

    public ProductVersion createProductVersion(Integer productId, String version) {
        ProductVersion productVersion = ProductVersion.builder()
                .version(version)
                .product(ProductRef.refBuilder().id(productId).build())
                .build();
        return failOnError(() -> versionEndpoint.createNewProductVersion(productVersion));
    }

    public GroupConfiguration createBuildConfigGroup(Integer versionId, String groupName) {
        GroupConfiguration groupConfig = GroupConfiguration.builder()
                .name(groupName)
                .productVersion(ProductVersionRef.refBuilder().id(versionId).build())
                .build();
        return failOnError(() -> configGroupEndpoint.createNew(groupConfig));
    }

    public Optional<BuildConfiguration> getBuildConfigByName(String name) {
        return null;  // TODO: Customise this generated block
    }

    private <V> Collection<V> toCollection(Iterable<V> iterable) {
        Collection<V> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    public void removeBuildConfigDependency(Integer id, Integer dependencyId) {
        failOnError(() -> {
            configEndpoint.removeDependency(id, dependencyId);
            return null;
        });
    }

    public void addBuildConfigDependency(Integer id, Integer dependencyId) {
        failOnError(() -> {
            BuildConfigurationRef configRef = BuildConfigurationRef.refBuilder().id(dependencyId).build();
            configEndpoint.addDependency(id, configRef);
            return null;
        });
    }

    public BuildConfiguration createBuildConfiguration(BuildConfiguration config) {
        return failOnError(() -> configEndpoint.createNew(config));
    }

    public void setBuildConfigurationsForGroup(int buildGroupId, Collection<Integer> configIds) {
        Collection<Integer> existingIds =
                failOnError(() -> toCollection(configGroupEndpoint.getConfigurations(buildGroupId)))
                        .stream()
                        .map(BuildConfiguration::getId)
                        .collect(Collectors.toList());

        existingIds.stream()
                .filter(c -> !configIds.contains(c))
                .forEach(c -> failOnError(() -> {
                    configGroupEndpoint.removeConfiguration(buildGroupId, c);
                    return null;
                }));

        configIds.stream()
                .filter(c -> !existingIds.contains(c))
                .forEach(
                        c ->
                                failOnError(() -> {
                                    configGroupEndpoint.addConfiguration(buildGroupId,
                                            BuildConfiguration.builder().id(c).build());
                                    return null;
                                })
                );
    }

    public SCMRepository createRepository(SCMRepository scmRepository) {
//        CreateAndSyncSCMRequest request = CreateAndSyncSCMRequest.builder()
//                .scmUrl(scmRepository.getExternalUrl())
//                .preBuildSyncEnabled(scmRepository.getPreBuildSyncEnabled())
//                .build();
//
//        return failOnError( () -> repositoryEndpoint.createNew(request));
        throw new TodoException();
    }

    private <V> V failOnError(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new PigException("Failed to get communicate with PNC", e);
        }
    }
}
