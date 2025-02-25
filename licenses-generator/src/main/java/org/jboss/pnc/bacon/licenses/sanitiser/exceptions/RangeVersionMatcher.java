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

package org.jboss.pnc.bacon.licenses.sanitiser.exceptions;

import java.util.Objects;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

class RangeVersionMatcher implements VersionMatcher {
    private final VersionScheme scheme;
    private final VersionConstraint constraint;

    RangeVersionMatcher(String spec) {
        Objects.requireNonNull(spec, "version range spec must be set");
        scheme = new GenericVersionScheme();
        try {
            constraint = scheme.parseVersionConstraint(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean matches(String version) {
        try {
            return constraint.containsVersion(scheme.parseVersion(version));
        } catch (InvalidVersionSpecificationException e) {
            return false;
        }
    }
}
