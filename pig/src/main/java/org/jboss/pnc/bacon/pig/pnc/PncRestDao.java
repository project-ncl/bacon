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

import lombok.experimental.Delegate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.pnc.bacon.pig.config.build.Product;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.GroupConfiguration;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductRef;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.Project;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.rest.api.endpoints.BuildConfigurationEndpoint;
import org.jboss.pnc.rest.api.endpoints.GroupConfigurationEndpoint;
import org.jboss.pnc.rest.api.endpoints.ProductEndpoint;
import org.jboss.pnc.rest.api.endpoints.ProductMilestoneEndpoint;
import org.jboss.pnc.rest.api.endpoints.ProductVersionEndpoint;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private BuildConfigurationEndpoint configEndpoint;
    private GroupConfigurationEndpoint configGroupEndpoint;
    private ProductEndpoint productEndpoint;
    private ProductVersionEndpoint versionEndpoint;
    private ProductMilestoneEndpoint milestoneEndpoint;


    public Optional<BuildConfiguration> getBuildConfig(Integer id) { // 120
        BuildConfiguration content = configEndpoint.getSpecific(id).getContent();
        return Optional.ofNullable(content);
    }

    public Collection<BuildConfiguration> listBuildConfigsInGroup(Integer groupId) {
        return configGroupEndpoint.getConfigurations(groupId, null).getContent();
    }

    @SuppressWarnings("rawtypes")
    public void markMilestoneCurrent(Integer versionId, Integer milestoneId) {
        // todo
    }

    protected Map<?, ?> getProductVersionAsMap(Integer versionId) {
        // todo
    }

    public Optional<ProductMilestone> getMilestoneIdForVersionAndName(Integer versionId, String milestoneName) {
        List<Map> result = client.getFromAllPages(
                milestonesForVersion(versionId),
                Map.class,
                pair("q", "version==" + milestoneName));

        return result.size() > 0
                ? Optional.of((Integer) result.iterator().next().get("id"))
                : Optional.empty();
    }

    public ProductMilestone createMilestone(Integer versionId, String milestoneName, String issueTrackerUrl) {
        ProductMilestone milestone = ProductMilestone.builder()
                .productVersion(ProductVersionRef.refBuilder().id(versionId).build())
                .version(milestoneName)
                .issueTrackerUrl(issueTrackerUrl)
                .startingDate(Instant.from(START_DATE)) // TODO: is it proper?
                .endDate(Instant.from(END_DATE))
                .build();
        return milestoneEndpoint.createNew(milestone).getContent();
    }

    public Optional<org.jboss.pnc.dto.Product> getProductByName(String name) {
        //todo
        return getOptionalMatch(urls.products(), "name==" + name, PncProduct.class);
    }

    public Optional<ProductVersion> getProductVersion(int productId, String majorMinor) {
        // todo
        return getOptionalMatch(urls.versionsForProduct(productId), "version==" + majorMinor, PncProductVersion.class);
    }

    public Optional<Project> getProjectByName(String name) {
        return getOptionalMatch(urls.projects(), "name==" + name, PncProject.class);
    }

    private <T> Optional<T> getOptionalMatch(String url, String query, Class<T> resultClass) {
        List<T> products = client.getFromAllPages(url, resultClass, pair("q", query));
        switch (products.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(products.iterator().next());
            default:
                throw new RuntimeException("Expected at most one match for url: " + url + " and query: " + query + ", got " + products.size());
        }
    }

    public Optional<PncRepositoryConfiguration> getRepositoryConfigurationByScmUri(String shortScmURIPath) {
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
        return productEndpoint.createNew(pncProduct).getContent();
    }

    public ProductVersion createProductVersion(Integer productId, String version) {
        ProductVersion productVersion = ProductVersion.builder()
                .version(version)
                .productId(ProductRef.refBuilder().id(productId).build())
                .build();
        return versionEndpoint.createNewProductVersion(productVersion).getContent();
    }

    public GroupConfiguration createBuildConfigGroup(Integer versionId, String groupName) {
        GroupConfiguration groupConfig = GroupConfiguration.builder()
                .name(groupName)
                .productVersion(ProductVersionRef.refBuilder().id(versionId).build())
                .build();
        return configGroupEndpoint.createNew(groupConfig).getContent();
    }

    public Optional<BuildConfiguration> getBuildConfigByName(String name) {
        return null;  // TODO: Customise this generated block
    }
}
