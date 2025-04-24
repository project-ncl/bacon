package org.jboss.pnc.bacon.licenses.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class OfflineLicenseMirrorTest {

    @Test
    void testLgpl21IsPresent() throws IOException {
        Optional<InputStream> license = OfflineLicenseMirror
                .find("http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html");
        assertTrue(license.isPresent());
        String value = IOUtils.toString(license.get(), StandardCharsets.UTF_8);
        assertNotNull(value);
        assertFalse(value.isBlank());
        assertTrue(value.contains("2.1"));
    }

    @Test
    void testLgpl21IsPresentWithHttps() throws IOException {
        Optional<InputStream> license = OfflineLicenseMirror
                .find("https://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html");
        assertTrue(license.isPresent());
        String value = IOUtils.toString(license.get(), StandardCharsets.UTF_8);
        assertNotNull(value);
        assertFalse(value.isBlank());
        assertTrue(value.contains("2.1"));
    }

    @Test
    void testLgpl21IsPresentForRepositoryJbossOrg() throws IOException {
        Optional<InputStream> license = OfflineLicenseMirror.find("http://repository.jboss.org/licenses/lgpl-2.1.txt");
        assertTrue(license.isPresent());
        String value = IOUtils.toString(license.get(), StandardCharsets.UTF_8);
        assertNotNull(value);
        assertFalse(value.isBlank());
        assertTrue(value.contains("2.1"));
    }

    @Test
    void testFakeLicenseReturnsEmpty() throws IOException {
        Optional<InputStream> license = OfflineLicenseMirror.find("https://dustin.com/license/open");
        assertFalse(license.isPresent());
    }

}
