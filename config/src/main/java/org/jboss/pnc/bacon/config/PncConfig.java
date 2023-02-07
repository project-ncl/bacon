/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/17/18
 */
@Data
@Slf4j
public class PncConfig implements Validate {

    private String url;

    private String bifrostBaseurl;

    @Override
    public void validate() {
        checkAndLog(url);
        Validate.validateUrl(url, "PNC");
        Validate.validateUrl(bifrostBaseurl, "Bifrost");
    }

    private void checkAndLog(String url) {
        try {
            URI uri = new URI(url);
            if (!uri.getPath().isEmpty()) {
                log.error("Check if your " + url + " url is correct, usually there should not be path part");
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not parse:", e);
        }
    }
}
