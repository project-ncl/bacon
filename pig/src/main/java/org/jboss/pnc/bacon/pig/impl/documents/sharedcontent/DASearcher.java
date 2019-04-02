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

package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.DADao;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.DAListArtifact;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/20/17
 */
public class DASearcher {
    private Multimap<GAV, DAListArtifact> whitelisted;
    private DADao da = DADao.getInstance();

    public DASearcher() {
        List<DAListArtifact> whitelist = da.getWhitelist();
        whitelisted = Multimaps.index(whitelist, DAListArtifact::getGav);
    }

    public void fillDAData(SharedContentReportRow row) {
        GAV gav = row.getGav();
        Collection<DAListArtifact> artifacts = whitelisted.get(gav);
        if (isNotEmpty(artifacts)) {
            List<String> productNames = new ArrayList<>();
            List<String> productVersions = new ArrayList<>();
            artifacts.forEach(a -> {
                productNames.add(a.getProductName());
                productVersions.add(a.getProductVersion());
            });
            row.setProductName(join(productNames, ','));
            row.setProductVersion(join(productVersions, ','));
        }
    }
}
