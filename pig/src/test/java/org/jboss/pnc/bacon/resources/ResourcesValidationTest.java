package org.jboss.pnc.bacon.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourcesValidationTest {

    ObjectMapper jsonMapper = new ObjectMapper();
    XmlMapper xmlMapper = new XmlMapper();

    @Test
    void resourceJsonFilesShouldBeJsonParsable() throws Exception {
        assertTrue(isJsonValid(getClass().getClassLoader().getResourceAsStream("rh-license-exceptions.json")));
        assertTrue(isJsonValid(getClass().getClassLoader().getResourceAsStream("rh-license-names.json")));
    }

    @Test
    void resourceXMLFilesShouldBeXMLParsable() {
        assertTrue(isXMLValid(getClass().getClassLoader().getResourceAsStream("indy-settings.xml")));
        assertTrue(isXMLValid(getClass().getClassLoader().getResourceAsStream("indy-temp-settings.xml")));
        assertTrue(isXMLValid(getClass().getClassLoader().getResourceAsStream("pom-template.xml")));
        assertTrue(isXMLValid(getClass().getClassLoader().getResourceAsStream("repository-example-settings.xml")));
        assertTrue(isXMLValid(getClass().getClassLoader().getResourceAsStream("settings-template.xml")));
    }

    @Test
    void testValidRegexInRhLicenseExceptionsJsonFile() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("rh-license-exceptions.json");
        List<LicenseEntry> items = jsonMapper.readValue(in, new TypeReference<List<LicenseEntry>>() {
        });
        for (LicenseEntry item : items) {
            if (item.versionRegexp != null) {
                assertTrue(isRegexValid(item.versionRegexp));
            }
        }
    }

    /**
     * Check whether the string is a valid JSON content or not
     *
     * @param content
     * @return whether valid or not
     */
    private boolean isJsonValid(InputStream content) {
        try {
            jsonMapper.readTree(content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check whether the string is a valid XML content or not
     *
     * @param content
     * @return whether valid or not
     */
    private boolean isXMLValid(InputStream content) {
        try {
            xmlMapper.readTree(content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Validate if regex has correct syntax or not
     *
     * @param regex
     * @return true or false
     */
    private boolean isRegexValid(String regex) {
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException exception) {
            return false;
        }
    }

    /**
     * DTO to validate whether the 'version-regexp' field in the license exception can be parsed as a Java Regex
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LicenseEntry {
        @JsonProperty("version-regexp")
        String versionRegexp;
    }
}
