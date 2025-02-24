/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

package org.jboss.pnc.bacon.pig.impl.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.io.FilenameUtils;
import org.jboss.da.listings.model.rest.RestArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/19/17
 */
@Getter
@EqualsAndHashCode(exclude = { "packaging", "classifier", "scope" })
@ToString
public class GAV {
    private static final Logger log = LoggerFactory.getLogger(GAV.class);

    public static final Comparator<GAV> gapvcComparator = Comparator.comparing(GAV::toGapvc);
    /**
     * See NCL-7238. Some gavs have no packaging due to weird Maven behaviour. In PNC, we need to specify a packaging to
     * generate a valid PURL and to avoid duplicates of artifacts. Therefore we chose to use the "empty" packaging.
     *
     * Also specified in repository-driver
     */
    public static final String FILE_NO_EXTENSION_PACKAGING = "empty";

    private String packaging; // not final for jackson
    private String groupId; // not final for jackson
    private String artifactId; // not final for jackson
    private String version; // not final for jackson
    private String scope;
    private String classifier;
    // Extensions with no "standard" format, like having a dot on it
    List<String> extensionExceptions = Arrays.asList("tar.gz", "tar.bz2");

    @Deprecated // for jackson
    public GAV() {

    }

    public GAV(String artifactPath) {
        log.debug("parsing artifact path {}", artifactPath);

        final String path = FilenameUtils.normalizeNoEndSeparator(artifactPath, true);

        int versionEnd = path.lastIndexOf('/');
        int artifactEnd = path.lastIndexOf('/', versionEnd - 1);
        int groupEnd = path.lastIndexOf('/', artifactEnd - 1);

        try {
            version = path.substring(artifactEnd + 1, versionEnd);
            artifactId = path.substring(groupEnd + 1, artifactEnd);
            groupId = path.substring(0, groupEnd).replaceAll("/", ".");

            int fileNameVersionEnd = versionEnd + artifactId.length() + version.length() + 2;
            if (path.charAt(fileNameVersionEnd) == '.') {
                packaging = path.substring(fileNameVersionEnd + 1);
            } else {
                packaging = extensionExceptions.stream()
                        .filter(e -> path.endsWith("." + e))
                        .findFirst()
                        .orElse(path.substring(path.lastIndexOf('.') + 1));
                classifier = path.substring(fileNameVersionEnd + 1, path.length() - packaging.length() - 1);
            }
        } catch (StringIndexOutOfBoundsException parsingException) {
            throw new RuntimeException("Unable to parse path " + path + " to artifact", parsingException);
        }
    }

    public GAV(String groupId, String artifactId, String version, String packaging) {
        this(groupId, artifactId, version, packaging, null);
    }

    public GAV(String groupId, String artifactId, String version, String packaging, String classifier) {
        this.packaging = packaging;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        // other methods assume that a non-null classifer is a non-empty classifier
        this.classifier = classifier == null || classifier.isEmpty() ? null : classifier;
    }

    public static GAV fromXml(Element xml, Map<String, String> properties) {
        String groupId = XmlUtils.getValue(xml, "groupId", properties);
        String artifactId = XmlUtils.getValue(xml, "artifactId", properties);
        String version = XmlUtils.getValue(xml, "version", properties);
        String packaging = XmlUtils.getValue(xml, "packaging", properties);
        String type = XmlUtils.getValue(xml, "type", properties);
        String scope = XmlUtils.getValue(xml, "scope", properties);
        String classifier = XmlUtils.getValue(xml, "classifier", properties);
        if (packaging == null && type != null) {
            packaging = type;
        }
        if (packaging == null) {
            packaging = "jar";
        }
        GAV gav = new GAV(groupId, artifactId, version, packaging, classifier);
        gav.scope = scope;
        return gav;
    }

    public static GAV fromDaGav(RestArtifact gav) {
        return new GAV(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), null);
    }

    public static GAV fromKojiArchive(KojiArchiveInfo archive) {
        return new GAV(archive.getGroupId(), archive.getArtifactId(), archive.getVersion(), archive.getExtension());
    }

    public String toGav() {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }

    public String toGapv() {
        return String.format("%s:%s:%s:%s", groupId, artifactId, packaging, version);
    }

    public String toGapvc() {
        if (classifier == null) {
            return String.format("%s:%s:%s:%s", groupId, artifactId, packaging, version);
        } else {
            return String.format("%s:%s:%s:%s:%s", groupId, artifactId, packaging, version, classifier);
        }
    }

    public String toVersionPath() {
        return String.format("%s/%s/%s", groupId.replace('.', '/'), artifactId, version);
    }

    public String toUri() {
        return String.format("%s/%s", toVersionPath(), toFileName());
    }

    public String toFileName() {
        if (FILE_NO_EXTENSION_PACKAGING.equals(packaging)) {
            if (classifier == null) {
                return String.format("%s-%s", artifactId, version);
            } else {
                return String.format("%s-%s-%s", artifactId, version, classifier);
            }
        } else {
            if (classifier == null) {
                return String.format("%s-%s.%s", artifactId, version, packaging);
            } else {
                return String.format("%s-%s-%s.%s", artifactId, version, classifier, packaging);
            }
        }
    }

    public String toPNCIdentifier() {
        String result = String.format("%s:%s:%s:%s", groupId, artifactId, packaging, version);
        if (classifier != null) {
            result += ":";
            result += classifier;
        }
        return result;
    }

    public static GAV fromColonSeparatedGAV(String colonSeparatedGav) {
        String[] split = colonSeparatedGav.split(":");
        return new GAV(split[0].trim(), split[1].trim(), split[2].trim(), null);
    }

    public static GAV fromColonSeparatedGAPV(String colonSeparatedGav) {
        String[] split = colonSeparatedGav.split(":");
        switch (split.length) {
            case 4:
                return new GAV(split[0].trim(), split[1].trim(), split[3].trim(), split[2].trim());
            case 5:
                return new GAV(split[0].trim(), split[1].trim(), split[3].trim(), split[2].trim(), split[4].trim());
            default:
                throw new RuntimeException(
                        "Error parsing gav: " + colonSeparatedGav
                                + ". Expected groupId:artifactId:packaging:classifier:version or groupId:artifactId:packaging:version");

        }
    }

    public String asBomXmlDependency() {
        // don't include BOM artifacts
        if ("import".equals(scope)) {
            return "";
        }

        return String.format(
                "      <dependency>\n" + "         <groupId>%s</groupId>\n" + "         <artifactId>%s</artifactId>\n"
                        + "         <type>%s</type>\n"
                        + (scope != null ? "         <scope>" + scope + "</scope>\n" : "")
                        + (classifier != null ? "         <classifier>" + classifier + "</classifier>\n" : "")
                        + "      </dependency>",
                groupId,
                artifactId,
                packaging);
    }

    public static GAV fromFileName(String absolutePath, String repoRootName) {
        absolutePath = FilenameUtils.normalize(absolutePath, true);
        repoRootName = FilenameUtils.normalize(repoRootName, true);
        int repoDirNameIdx = absolutePath.lastIndexOf(repoRootName);
        String gavPart = absolutePath.substring(repoDirNameIdx + repoRootName.length());

        return new GAV(gavPart);
    }

    public String getGa() {
        return String.format("%s:%s", groupId, artifactId);
    }

    public boolean matches(String expression) {
        return toGapvc().matches(expression);
    }

    public boolean isTemporary() {
        return isTempVersion(version);
    }

    public boolean isNormalJar() {
        return isBlank(classifier) && packaging.equals("jar");
    }

    public GAV toSourcesJar() {
        return new GAV(groupId, artifactId, version, "jar", "sources");
    }

    public GAV toJar() {
        return new GAV(groupId, artifactId, version, "jar");
    }

    public GAV toPom() {
        return new GAV(groupId, artifactId, version, "pom");
    }

    public GAV toJavadocJar() {
        return new GAV(groupId, artifactId, version, "jar", "javadoc");
    }

    public boolean isCommunity() {
        return !version.contains("redhat");
    }

    public static boolean isTempVersion(String v) {
        return v.contains("temporary-redhat") || v.matches(".*\\.t\\d{8}-\\d+-\\d+-redhat-\\d+");
    }
}
