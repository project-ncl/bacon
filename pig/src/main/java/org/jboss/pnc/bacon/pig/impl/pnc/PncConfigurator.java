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

import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.toStream;

import java.io.Closeable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductMilestoneClient;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductMilestoneRef;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/14/17
 */
public class PncConfigurator implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(PncConfigurator.class);

    private static final Instant START_DATE = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Instant END_DATE = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

    private final ProductMilestoneClient milestoneClient;
    private final ProductVersionClient versionClient;

    public PncConfigurator() {
        // TODO: pull out creating all the clients to one factory?
        milestoneClient = new ProductMilestoneClient(PncClientHelper.getPncConfiguration());
        versionClient = new ProductVersionClient(PncClientHelper.getPncConfiguration());
    }

    public ProductMilestone getOrGenerateMilestone(ProductVersionRef version, String milestone) {
        log.info("Generating milestone for versionId {} and milestone {} in PNC", version, milestone);

        return getExistingMilestone(version, milestone).orElseGet(() -> createMilestone(version, milestone));
    }

    public void markMilestoneCurrent(ProductVersion version, ProductMilestoneRef milestone) {

        ProductVersion updated = version.toBuilder().currentProductMilestone(milestone).build();
        try {
            versionClient.update(version.getId(), updated);
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to set milestone current");
        }
    }

    public Optional<ProductMilestone> getExistingMilestone(ProductVersionRef version, String milestone) {
        String milestoneName = milestone;
        RemoteCollection<ProductMilestone> milestones = null;
        try {
            milestones = versionClient
                    .getMilestones(version.getId(), Optional.empty(), Optional.of("version==" + milestoneName));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Error getting milestone for milestoneName: " + milestoneName, e);
        }

        return toStream(milestones).findAny();
    }

    private ProductMilestone createMilestone(ProductVersionRef version, String milestoneName) {
        ProductMilestone milestone = ProductMilestone.builder()
                .productVersion(version)
                .startingDate(START_DATE)
                .plannedEndDate(END_DATE)
                .version(milestoneName)
                .build();

        try {
            return milestoneClient.createNew(milestone);
        } catch (ClientException e) {
            throw new RuntimeException("Error creating milestone " + milestoneName, e);
        }
    }

    @Override
    public void close() {
        milestoneClient.close();
        versionClient.close();
    }
}
