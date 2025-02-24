/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.experimental.Delegate;

import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/19/17
 */
@Data
public class SharedContentReportRow {
    @Delegate
    private final GAV gav;
    // private String classifier; TODO?

    private String productName;
    private String productVersion;
    private Boolean released;

    private String buildAuthor;
    private String buildId;
    private List<String> buildTags;

    private Path filePath;

    public SharedContentReportRow(File file, String repoDirName) {
        gav = GAV.fromFileName(file.getAbsolutePath(), repoDirName);

        filePath = file.toPath();
    }

    /**
     * for test usage only!
     *
     * @param gav no description
     */
    @Deprecated
    protected SharedContentReportRow(GAV gav) {
        this.gav = gav;
    }

    /**
     * Appends g:a:p:v;productName;productVersion;isReleased;buildUrl;buildAuthor;comma,separated,tags to the given
     * buffer (string builder)
     *
     * @param builder output
     */
    public void printTo(StringBuilder builder) {
        builder.append(toGapv()).append(";");
        builder.append(productName).append(";");
        builder.append(productVersion).append(";");
        builder.append(released).append(";");
        builder.append(buildId).append(";");
        builder.append(buildAuthor).append(";");
        String candidateTags = buildTags != null
                ? buildTags.stream().filter(t -> t.contains("-candidate")).collect(Collectors.joining(","))
                : "";
        builder.append(candidateTags).append(";");
        builder.append(StringUtils.join(buildTags, ",")).append("\n");
    }

    private String toComparableString() {
        return defaultIfNull(productName, "") + "|" + defaultIfNull(productVersion, "") + "|" + toGav();
    }

    public static int byProductAndGav(SharedContentReportRow row1, SharedContentReportRow row2) {
        return row1.toComparableString().compareTo(row2.toComparableString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SharedContentReportRow that = (SharedContentReportRow) o;
        return Objects.equals(gav, that.gav);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gav);
    }
}
