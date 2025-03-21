/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jboss.bacon.da.DaHelper;
import org.jboss.bacon.da.rest.endpoint.ListingsApi;
import org.jboss.bacon.da.rest.endpoint.ReportsApi;
import org.jboss.da.listings.model.rest.RestProductGAV;
import org.jboss.da.reports.model.request.LookupGAVsRequest;
import org.jboss.da.reports.model.response.LookupReport;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.DaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/2/17
 *         <p>
 *         TODO: unify with DA common client code when ready
 */
public class DADao {
    private static final Logger log = LoggerFactory.getLogger(DADao.class);

    private final ReportsApi reportsClient;
    private final ListingsApi listingsClient;

    public DADao(DaConfig daConfig) {
        reportsClient = DaHelper.createReportsApi();
        listingsClient = DaHelper.createListingsApi();
    }

    public void fillDaData(CommunityDependency dependency) {
        log.debug("Analyzing: {}", dependency);
        LookupGAVsRequest lookupRequest = new LookupGAVsRequest(Collections.singletonList(dependency.toDaGav()));
        List<LookupReport> lookupReports = reportsClient.lookupGav(lookupRequest);
        LookupReport lookupReport = getSingle(lookupReports);
        String bestMatchVersion = lookupReport.getBestMatchVersion();
        String availableVersions = String.join(",", lookupReport.getAvailableVersions());

        if (StringUtils.isNotBlank(bestMatchVersion)) {
            dependency.setState(DependencyState.MATCH_FOUND);
            dependency.setRecommendation(bestMatchVersion);
            dependency.setAvailableVersions(availableVersions);
        } else if (StringUtils.isNotBlank(availableVersions)) {
            dependency.setState(DependencyState.REVERSION_POSSIBLE);
            dependency.setAvailableVersions(availableVersions);
        } else {
            dependency.setState(DependencyState.NO_MATCH);
            dependency.setAvailableVersions("None");
        }
    }

    private LookupReport getSingle(List<LookupReport> lookupReports) {
        if (lookupReports.size() != 1) {
            throw new RuntimeException("Expected exactly one report, got: " + lookupReports.size());
        }
        return lookupReports.get(0);
    }

    public List<DAListArtifact> getWhitelist() {
        Collection<RestProductGAV> allWhiteArtifacts = listingsClient.getAllWhiteArtifacts();

        return allWhiteArtifacts.stream().map(DAListArtifact::new).collect(Collectors.toList());
    }

    private static DADao instance;

    public static synchronized DADao getInstance() {
        if (instance == null) {
            instance = new DADao(Config.instance().getActiveProfile().getDa());
        }
        return instance;
    }

}
