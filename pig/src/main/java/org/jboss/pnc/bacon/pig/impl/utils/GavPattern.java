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

import java.io.Serializable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A general purpose pattern for matching GAVs (i.e. quintupless consisting of {@code groupId}, {@code artifactId},
 * {@code type} {@code classifier} and {@code version}).
 * <p>
 * To create a new {@link GavPattern}, use either {@link #of(String)} or {@link #builder()}, both of which accept
 * wildcard patterns (rather than regular expression patterns). See the JavaDocs of the two respective methods for more
 * details.
 * <p>
 * {@link GavPattern} overrides {@link #hashCode()} and {@link #equals(Object)} and can thus be used as a key in a
 * {@link Map}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class GavPattern implements Serializable {

    /**
     * A {@link GavPattern} builder.
     */
    public static class Builder {

        private GavSegmentPattern groupIdPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern artifactIdPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern typePattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern classifierPattern = GavSegmentPattern.MATCH_ALL;
        private GavSegmentPattern versionPattern = GavSegmentPattern.MATCH_ALL;

        private Builder() {
        }

        public GavPattern build() {
            return new GavPattern(groupIdPattern, artifactIdPattern, typePattern, classifierPattern, versionPattern);
        }

        /**
         * Sets the pattern for {@code groupId}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder groupIdPattern(String wildcardPattern) {
            this.groupIdPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code artifactId}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder artifactIdPattern(String wildcardPattern) {
            this.artifactIdPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code classifier}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder classifierPattern(String wildcardPattern) {
            this.classifierPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code version}
         *
         * @param wildcardPattern a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder versionPattern(String wildcardPattern) {
            this.versionPattern = new GavSegmentPattern(wildcardPattern);
            return this;
        }

    }

    /**
     * A pair of a {@link Pattern} and its wildcard source.
     */
    static class GavSegmentPattern implements Serializable {
        private static final GavSegmentPattern MATCH_ALL = new GavSegmentPattern(GavPattern.MULTI_WILDCARD);
        private static final String MATCH_ALL_PATTERN_SOURCE = ".*";
        /**  */
        private static final long serialVersionUID = 1063634992004995585L;
        private final transient Pattern pattern;
        private final String source;

        GavSegmentPattern(String wildcardSource) {
            super();
            final StringBuilder sb = new StringBuilder(wildcardSource.length() + 2);
            final StringTokenizer st = new StringTokenizer(wildcardSource, GavPattern.MULTI_WILDCARD, true);
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (GavPattern.MULTI_WILDCARD.equals(token)) {
                    sb.append(MATCH_ALL_PATTERN_SOURCE);
                } else {
                    sb.append(Pattern.quote(token));
                }
            }
            this.pattern = Pattern.compile(sb.toString());
            this.source = wildcardSource;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GavSegmentPattern other = (GavSegmentPattern) obj;
            return source.equals(other.source);
        }

        /**
         * @return the wildcard source of the {@link #pattern}
         */
        public String getSource() {
            return source;
        }

        @Override
        public int hashCode() {
            return source.hashCode();
        }

        public boolean matches(String input) {
            if (input == null) {
                /* null input returns true only if the pattern is * */
                return MATCH_ALL.equals(this);
            }
            return pattern.matcher(input).matches();
        }

        /**
         * @return {@code true} if this {@link GavSegmentPattern} is equal to {@link #MATCH_ALL}; {@code false}
         *         otherwise
         */
        public boolean matchesAll() {
            return MATCH_ALL.equals(this);
        }

        @Override
        public String toString() {
            return source;
        }
    }

    private static final char DELIMITER = ':';
    private static final String DELIMITER_STRING;
    private static final GavPattern MATCH_ALL;
    private static final GavPattern MATCH_SNAPSHOTS;
    static final String MULTI_WILDCARD;
    static final String MULTI_WILDCARD_CHAR = "*";
    private static final long serialVersionUID = 5570763687443531797L;
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    static {
        MULTI_WILDCARD = String.valueOf(MULTI_WILDCARD_CHAR);
        DELIMITER_STRING = String.valueOf(DELIMITER);
        MATCH_ALL = new GavPattern(
                GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL);
        MATCH_SNAPSHOTS = new GavPattern(
                GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL,
                GavSegmentPattern.MATCH_ALL,
                new GavSegmentPattern(MULTI_WILDCARD + SNAPSHOT_SUFFIX));
    }

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a singleton that matches all possible GAVs
     */
    public static GavPattern matchAll() {
        return MATCH_ALL;
    }

    /**
     * @return a singleton that matches any GAV that has a version ending with {@value #SNAPSHOT_SUFFIX}
     */
    public static GavPattern matchSnapshots() {
        return MATCH_SNAPSHOTS;
    }

    /**
     * Creates a new {@link GavPattern} out of the given {@code wildcardPattern}. A wildcard pattern consists of string
     * literals and asterisk wildcard {@code *}. {@code *} matches zero or many arbitrary characters. Wildcard patterns
     * for groupId, artifactId, classifier and version need to be delimited by colon {@value #DELIMITER}.
     * <p>
     * The general syntax of a {@link GavPattern} follows the pattern
     * <code>groupIdPattern:[artifactIdPattern:[[typePattern:classifierIdPattern]:versionPattern]]</code>. Note that
     * type and classifier need to be specified both or none and that they may occur on the third and fourth position
     * respectively. Hence a {@link GavPattern} with three segments {@code org.my-group:my-artifact:1.2.3} is a short
     * hand for {@code org.my-group:*:*:my-artifact:1.2.3} matching any type and any classifier.
     * <p>
     * GAV pattern examples:
     * <p>
     * {@code org.my-group} - an equivalent of {@code org.my-group:*:*:*}. It will match any version of any artifact
     * having groupId {@code org.my-group}.
     * <p>
     * {@code org.my-group*} - an equivalent of {@code org.my-group*:*:*:*}. It will match any version of any artifact
     * whose groupId starts with {@code org.my-group} - i.e. it will match all of {@code org.my-group},
     * {@code org.my-group.api}, {@code org.my-group.impl}, etc.
     * <p>
     * {@code org.my-group:my-artifact} - an equivalent of {@code org.my-group:my-artifact:*}. It will match any version
     * of all such artifacts that have groupId {@code org.my-group} and artifactId {@code my-artifact}
     * <p>
     * {@code org.my-group:my-artifact:1.2.3} - will match just the version 1.2.3 of artifacts
     * {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact:*:linux-x86_64:1.2.3} - will match artifacts of all types having classifier
     * linux-x86_64 and version 1.2.3 of {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact:*::1.2.3} - will match artifacts of all types having no classifier and version
     * 1.2.3 of {@code org.my-group:my-artifact}.
     * <p>
     * {@code org.my-group:my-artifact:jar:1.2.3} - Illegal because both type and classifier have to be specified.
     * <p>
     * {@code org.my-group:my-artifact:jar::1.2.3} - will match the jar having no classifier and version 1.2.3 of
     * {@code org.my-group:my-artifact}.
     *
     * @param wildcardPattern a string pattern to parse and create a new {@link GavPattern} from
     * @return a new {@link GavPattern}
     */
    public static GavPattern of(String wildcardPattern) {
        final GavSegmentPattern groupIdPattern;
        StringTokenizer st = new StringTokenizer(wildcardPattern, DELIMITER_STRING);
        if (st.hasMoreTokens()) {
            groupIdPattern = new GavSegmentPattern(st.nextToken());
        } else {
            groupIdPattern = GavSegmentPattern.MATCH_ALL;
        }
        final GavSegmentPattern artifactIdPattern;
        if (st.hasMoreTokens()) {
            artifactIdPattern = new GavSegmentPattern(st.nextToken());
        } else {
            artifactIdPattern = GavSegmentPattern.MATCH_ALL;
        }
        final GavSegmentPattern typePattern;
        final GavSegmentPattern classifierPattern;
        final GavSegmentPattern versionPattern;
        if (st.hasMoreTokens()) {
            final String third = st.nextToken();
            if (st.hasMoreTokens()) {
                final String fourth = st.nextToken();
                if (st.hasMoreTokens()) {
                    final String fifth = st.nextToken();
                    typePattern = new GavSegmentPattern(third);
                    classifierPattern = new GavSegmentPattern(fourth);
                    versionPattern = new GavSegmentPattern(fifth);
                } else {
                    throw new IllegalStateException(
                            GavSegmentPattern.class.getName()
                                    + ".of() expects groupId:artifactId:version or groupId:artifactId:type:classifier:version; found: "
                                    + wildcardPattern);
                }
            } else {
                typePattern = GavSegmentPattern.MATCH_ALL;
                classifierPattern = GavSegmentPattern.MATCH_ALL;
                versionPattern = new GavSegmentPattern(third);
            }
        } else {
            typePattern = GavSegmentPattern.MATCH_ALL;
            classifierPattern = GavSegmentPattern.MATCH_ALL;
            versionPattern = GavSegmentPattern.MATCH_ALL;
        }
        return new GavPattern(groupIdPattern, artifactIdPattern, typePattern, classifierPattern, versionPattern);
    }

    final GavSegmentPattern groupIdPattern;
    final GavSegmentPattern artifactIdPattern;
    final GavSegmentPattern typePattern;
    final GavSegmentPattern classifierPattern;
    final GavSegmentPattern versionPattern;
    private final transient String source;

    GavPattern(
            GavSegmentPattern groupIdPattern,
            GavSegmentPattern artifactIdPattern,
            GavSegmentPattern typePattern,
            GavSegmentPattern classifierPattern,
            GavSegmentPattern versionPattern) {
        super();
        this.groupIdPattern = groupIdPattern;
        this.artifactIdPattern = artifactIdPattern;
        this.typePattern = typePattern;
        this.classifierPattern = classifierPattern;
        this.versionPattern = versionPattern;

        StringBuilder source = new StringBuilder(
                groupIdPattern.getSource().length() + artifactIdPattern.getSource().length()
                        + typePattern.getSource().length() + classifierPattern.getSource().length()
                        + versionPattern.getSource().length() + 3);

        source.append(groupIdPattern.getSource());
        final boolean artifactMatchesAll = artifactIdPattern.matchesAll();
        final boolean typeMatchesAll = typePattern.matchesAll();
        final boolean classifierMatchesAll = classifierPattern.matchesAll();
        final boolean versionMatchesAll = versionPattern.matchesAll();
        if (!versionMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
            if (!typeMatchesAll && !classifierMatchesAll) {
                source.append(DELIMITER).append(typePattern.getSource());
                source.append(DELIMITER).append(classifierPattern.getSource());
            }
            source.append(DELIMITER).append(versionPattern.getSource());
        } else if (!typeMatchesAll && !classifierMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
            source.append(DELIMITER).append(typePattern.getSource());
            source.append(DELIMITER).append(classifierPattern.getSource());
        } else if (!artifactMatchesAll) {
            source.append(DELIMITER).append(artifactIdPattern.getSource());
        }
        this.source = source.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GavPattern other = (GavPattern) obj;
        return this.source.equals(other.source);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code type}, {@code classifier}, {@code version}
     * quintuple against this {@link GavPattern}.
     *
     * @param groupId
     * @param artifactId
     * @param type cannot be {@code null}
     * @param classifier can be {@code null}
     * @param version
     * @return {@code true} if this {@link GavPattern} matches the given {@code groupId}, {@code artifactId},
     *         {@code type}, {@code classifier}, {@code version} quintuple and {@code false otherwise}
     */
    public boolean matches(String groupId, String artifactId, String type, String classifier, String version) {
        return groupIdPattern.matches(groupId) && //
                artifactIdPattern.matches(artifactId) && //
                typePattern.matches(type) && //
                classifierPattern.matches(classifier) && //
                versionPattern.matches(version);
    }

    @Override
    public String toString() {
        return source;
    }

}
