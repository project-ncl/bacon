/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
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
package org.jboss.pnc.bacon.licenses.sanitiser;

import org.jboss.pnc.bacon.licenses.sanitiser.provider.ExternalLicenseProvider;
import org.jboss.pnc.bacon.licenses.xml.DependencyElement;
import org.jboss.pnc.bacon.licenses.xml.LicenseElement;

import java.util.Set;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/15/17
 */
public class LicenseServiceSanitiser implements LicenseSanitiser {

    private final LicenseSanitiser next;
    private final ExternalLicenseProvider provider;

    public LicenseServiceSanitiser(String licenseServiceUrl, LicenseSanitiser next) {
        this.next = next;
        this.provider = new ExternalLicenseProvider(licenseServiceUrl);
    }

    @Override
    public DependencyElement fix(DependencyElement dependencyElement) {
        Set<LicenseElement> licenses = provider.getLicenses(dependencyElement.toGavString());
        if (!licenses.isEmpty()) {
            return new DependencyElement(dependencyElement, licenses);
        } else {
            return next.fix(dependencyElement);
        }
    }
}
