/**
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

package org.jboss.pnc.bacon.pig.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FilenameUtils;
import org.jboss.pnc.bacon.pig.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Comparator;
import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/19/17
 */
@Getter
@EqualsAndHashCode(exclude = {"packaging", "classifier", "scope"})
@ToString
public class GAV {
    private static final Logger log = LoggerFactory.getLogger(GAV.class);

    public static final Comparator<GAV> gapvcComparator = Comparator.comparing(GAV::toGapvc);

    private final String packaging;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private String scope;
    private String classifier;

    public GAV(String path) {
        log.debug("parsing artifact path {}", path);

        path = FilenameUtils.normalizeNoEndSeparator(path, true);

        int versionEnd = path.lastIndexOf('/');
        int artifactEnd = path.lastIndexOf('/', versionEnd - 1);
        int groupEnd = path.lastIndexOf('/', artifactEnd - 1);

        try {
            packaging = path.substring(path.lastIndexOf('.') + 1);
            version = path.substring(artifactEnd + 1, versionEnd);
            artifactId = path.substring(groupEnd + 1, artifactEnd);
            groupId = path.substring(0, groupEnd).replaceAll("/", ".");
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
        this.classifier = classifier;
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

    public String toGav() {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }

    public String toGapv() {
        return String.format("%s:%s:%s:%s", groupId, artifactId, packaging, version);
    }

    public String toGapvc() {
        return String.format("%s:%s:%s:%s:%s", groupId, artifactId, packaging, version, classifier);
    }

    public String toVersionPath() {
        return String.format("%s/%s/%s",
                groupId.replace('.', '/'), artifactId, version);
    }

    public String toUri() {
        return String.format("%s/%s",
                toVersionPath(),
                toFileName());
    }

    public String toFileName() {
        if (classifier == null) {
            return String.format("%s-%s.%s", artifactId, version, packaging);
        } else {
            return String.format("%s-%s-%s.%s", artifactId, version, classifier, packaging);
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
        String split[] = colonSeparatedGav.split(":");
        return new GAV(split[0], split[1], split[2], null);
    }

    public static GAV fromColonSeparatedGAPV(String colonSeparatedGav) {
        String split[] = colonSeparatedGav.split(":");
        switch (split.length) {
            case 4:
                return new GAV(split[0], split[1], split[3], split[2]);
            case 5:
                return new GAV(split[0], split[1], split[3], split[2], split[4]);
            default:
                throw new RuntimeException("Error parsing gav: " + colonSeparatedGav
                        + ". Expected groupId:artifactId:packaging:classifier:version or groupId:artifactId:packaging:version");

        }
    }

    public String asBomXmlDependency() {
        //don't include BOM artifacts
        if ("import".equals(scope)) {
            return "";
        }

        return String.format("      <dependency>\n" +
                "         <groupId>%s</groupId>\n" +
                "         <artifactId>%s</artifactId>\n" +
                "         <type>%s</type>\n" +
                (scope != null ? "         <scope>" + scope + "</scope>\n" : "") +
                (classifier != null ? "         <classifier>" + classifier + "</classifier>\n" : "") +
                "      </dependency>", groupId, artifactId, packaging);
    }

    public static GAV fromFileName(String absolutePath, String repoRootName) {
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
}
