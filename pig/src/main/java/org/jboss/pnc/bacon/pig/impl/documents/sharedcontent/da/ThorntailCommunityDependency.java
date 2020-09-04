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
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
@Getter
@Setter
@ToString
public class ThorntailCommunityDependency implements CsvExportable {

    private final CommunityDependency communityDependency;
    private List<String> usedForThorntail;

    public ThorntailCommunityDependency(CommunityDependency communityDependency, List<String> usedForThorntail) {
        this.communityDependency = communityDependency;
        this.usedForThorntail = usedForThorntail;
    }

    public String toCsvLine() {
        return String.format("%s; %s", communityDependency.toCsvLine(), usedForThorntail);
    }

    public List<String> getUsedForThorntail() {
        return usedForThorntail;
    }
}
