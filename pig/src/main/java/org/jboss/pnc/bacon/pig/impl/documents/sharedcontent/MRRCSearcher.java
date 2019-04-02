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

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/19/17
 */
public class MRRCSearcher {
    private static final Logger log = LoggerFactory.getLogger(MRRCSearcher.class);

    private static final String mrrcBaseUrl = "https://maven.repository.redhat.com/ga/";

    private static final MRRCSearcher instance = new MRRCSearcher();

    public static MRRCSearcher getInstance() {
        return instance;
    }

    public void fillMRRCData(SharedContentReportRow row) {
        log.debug("Asking mrrc for {}\n", row.toGapv());
        row.setReleased(isReleased(row.getGav()));
    }

    public Boolean isReleased(GAV gav) {
        HttpHead request = new HttpHead(uriForRow(gav));
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(30_000);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(30_000);
        try (CloseableHttpClient client = HttpClientBuilder.create().build();
             CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode == 200;
        } catch (IOException e) {
            log.error("Failed to get data for {} from MRRC", gav, e);
            return null;
        }
    }

    private String uriForRow(GAV gav) {
        String uriSuffix = gav.toUri();
        return mrrcBaseUrl + uriSuffix;
    }
}
