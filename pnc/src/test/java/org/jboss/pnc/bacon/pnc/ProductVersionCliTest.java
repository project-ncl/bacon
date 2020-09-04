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
package org.jboss.pnc.bacon.pnc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductVersionCliTest {

    @Test
    void validateProductVersionTest() {

        assertThat(ProductVersionCli.validateProductVersion("1.2")).isTrue();
        assertThat(ProductVersionCli.validateProductVersion("1231.1232")).isTrue();
        assertThat(ProductVersionCli.validateProductVersion("1.1232")).isTrue();
        assertThat(ProductVersionCli.validateProductVersion("1000.1232")).isTrue();

        assertThat(ProductVersionCli.validateProductVersion("abc")).isFalse();
        assertThat(ProductVersionCli.validateProductVersion("1.2.3")).isFalse();
        assertThat(ProductVersionCli.validateProductVersion("42")).isFalse();
        assertThat(ProductVersionCli.validateProductVersion("42.1a")).isFalse();
    }
}
