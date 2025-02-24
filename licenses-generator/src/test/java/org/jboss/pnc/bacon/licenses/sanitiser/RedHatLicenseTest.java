package org.jboss.pnc.bacon.licenses.sanitiser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonObject;

import org.jboss.pnc.bacon.licenses.xml.LicenseElement;
import org.junit.jupiter.api.Test;

public class RedHatLicenseTest {

    private static JsonObject LICENSE_WITH_ALIASES = Json.createObjectBuilder()
            .add("name", "Apache License 2.0")
            .add("url", "http://www.apache.org/licenses/LICENSE-2.0")
            .add("textUrl", "http://www.apache.org/licenses/LICENSE-2.0.txt")
            .add("aliases", Json.createArrayBuilder(Arrays.asList("Apache License", "   ASL 2.0   ")))
            .add(
                    "urlAliases",
                    Json.createArrayBuilder(
                            Arrays.asList(
                                    "http://www.apache.org/licenses/LICENSE-2.0/",
                                    "https://apache.org/licenses/LICENSE-2.0",
                                    "www.apache.org/licenses/LICENSE-2.0.txt    ")))
            .build();

    private static JsonObject LICENSE_WITHOUT_ALIASES = Json.createObjectBuilder()
            .add("name", "MIT License")
            .add("url", "https://opensource.org/licenses/MIT")
            .build();

    @Test
    public void shouldLoadSanitisedData() {
        RedHatLicense redHatLicense = new RedHatLicense(LICENSE_WITH_ALIASES);
        assertThat(redHatLicense.getName()).isEqualTo("Apache License 2.0");
        assertThat(redHatLicense.getUrl()).isEqualTo("http://www.apache.org/licenses/LICENSE-2.0");
        assertThat(redHatLicense.getTextUrl()).isEqualTo("http://www.apache.org/licenses/LICENSE-2.0.txt");
        assertThat(redHatLicense.getAliases()).containsOnly("apache license", "asl 2.0");
        assertThat(redHatLicense.getUrlAliases())
                .containsOnly("apache.org/licenses/license-2.0", "apache.org/licenses/license-2.0.txt");

        redHatLicense = new RedHatLicense(LICENSE_WITHOUT_ALIASES);
        assertThat(redHatLicense.getName()).isEqualTo("MIT License");
        assertThat(redHatLicense.getUrl()).isEqualTo("https://opensource.org/licenses/MIT");
        assertThat(redHatLicense.getTextUrl()).isEqualTo("https://opensource.org/licenses/MIT");
        assertThat(redHatLicense.getAliases()).isEmpty();
        assertThat(redHatLicense.getUrlAliases()).isEmpty();
    }

    @Test
    public void shouldGetLicenseElement() {
        RedHatLicense redHatLicense = new RedHatLicense(LICENSE_WITH_ALIASES);
        LicenseElement licenseElement = redHatLicense.toLicenseElement();

        assertThat(licenseElement.getName()).isEqualTo("Apache License 2.0");
        assertThat(licenseElement.getUrl()).isEqualTo("http://www.apache.org/licenses/LICENSE-2.0");
        assertThat(licenseElement.getTextUrl()).isEqualTo("http://www.apache.org/licenses/LICENSE-2.0.txt");
    }

    @Test
    public void shouldRecogniseNameAlias() {
        RedHatLicense redHatLicense = new RedHatLicense(LICENSE_WITH_ALIASES);
        LicenseElement licenseElement = new LicenseElement("APACHE LICENSE", "example.com");

        assertThat(redHatLicense.isAliasTo(licenseElement)).isTrue();
    }

    @Test
    public void shouldRecogniseUrlAlias() {
        RedHatLicense redHatLicense = new RedHatLicense(LICENSE_WITH_ALIASES);
        LicenseElement licenseElement = new LicenseElement("Example", "https://apache.org/licenses/LICENSE-2.0/");

        assertThat(redHatLicense.isAliasTo(licenseElement)).isTrue();
    }

    @Test
    public void shouldNotRecognisedUnknownLicense() {
        RedHatLicense redHatLicense = new RedHatLicense(LICENSE_WITH_ALIASES);
        LicenseElement licenseElement = new LicenseElement("Example", "example.com");

        assertThat(redHatLicense.isAliasTo(licenseElement)).isFalse();
    }
}
