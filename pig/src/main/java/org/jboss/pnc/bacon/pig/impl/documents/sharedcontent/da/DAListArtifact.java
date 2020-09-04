/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import lombok.Getter;
import lombok.experimental.Delegate;
import org.jboss.da.listings.model.rest.RestProductGAV;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/20/17
 */
@Getter
public class DAListArtifact {
    @Delegate
    private GAV gav;
    private String productName;
    private String productVersion;
    private String state;

    public DAListArtifact(RestProductGAV daGavWithProduct) {
        gav = GAV.fromDaGav(daGavWithProduct.getGav());
        productName = daGavWithProduct.getName();
        productVersion = daGavWithProduct.getVersion();
        state = daGavWithProduct.getSupportStatus().name();
    }
}
