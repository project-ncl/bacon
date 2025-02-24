package org.jboss.pnc.bacon.pnc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
