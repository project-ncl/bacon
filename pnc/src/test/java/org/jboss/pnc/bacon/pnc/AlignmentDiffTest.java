/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlignmentDiffTest {

    private static final String POM_SECTION = String.join(
            "\n",
            "diff --git a/pom.xml b/pom.xml",
            "index 1111111..2222222 100644",
            "--- a/pom.xml",
            "+++ b/pom.xml",
            "@@ -1,1 +1,1 @@",
            "-<version>1.0.0</version>",
            "+<version>1.0.0.redhat-00001</version>");

    private static final String REPOSITORIES_BACKUP_SECTION = String.join(
            "\n",
            "diff --git a/repositories-backup.xml b/repositories-backup.xml",
            "new file mode 100644",
            "index 0000000..3333333",
            "--- /dev/null",
            "+++ b/repositories-backup.xml",
            "@@ -0,0 +1,1 @@",
            "+<settings/>");

    private static final String MANIPULATION_SECTION = String.join(
            "\n",
            "diff --git a/target/manipulation.json b/target/manipulation.json",
            "new file mode 100644",
            "index 0000000..4444444",
            "--- /dev/null",
            "+++ b/target/manipulation.json",
            "@@ -0,0 +1,1 @@",
            "+{}");

    private static final String ALIGNMENT_REPORT_SECTION = String.join(
            "\n",
            "diff --git a/alignmentReport.json b/alignmentReport.json",
            "new file mode 100644",
            "index 0000000..5555555",
            "--- /dev/null",
            "+++ b/alignmentReport.json",
            "@@ -0,0 +1,1 @@",
            "+{}");

    private static final String GME_REPOS_SECTION = String.join(
            "\n",
            "diff --git a/gradle/gme-repos.gradle b/gradle/gme-repos.gradle",
            "new file mode 100644",
            "index 0000000..6666666",
            "--- /dev/null",
            "+++ b/gradle/gme-repos.gradle",
            "@@ -0,0 +1,1 @@",
            "+// repos");

    @Test
    void shouldReturnEmptyPartsForNullOrEmpty() {
        assertEquals("", AlignmentDiff.split(null).buildAffecting());
        assertEquals("", AlignmentDiff.split(null).metadata());
        assertFalse(AlignmentDiff.split(null).hasMetadata());
        assertEquals("", AlignmentDiff.split("").buildAffecting());
    }

    @Test
    void shouldKeepEverythingAsBuildAffectingWhenNoMetadata() {
        AlignmentDiff diff = AlignmentDiff.split(POM_SECTION + "\n");

        assertEquals(POM_SECTION + "\n", diff.buildAffecting());
        assertEquals("", diff.metadata());
        assertFalse(diff.hasMetadata());
    }

    @Test
    void shouldRouteMetadataFilesToMetadataPart() {
        String unifiedDiff = POM_SECTION + "\n" + REPOSITORIES_BACKUP_SECTION + "\n" + MANIPULATION_SECTION + "\n"
                + ALIGNMENT_REPORT_SECTION + "\n" + GME_REPOS_SECTION + "\n";

        AlignmentDiff diff = AlignmentDiff.split(unifiedDiff);

        assertTrue(diff.hasMetadata());

        assertTrue(diff.buildAffecting().contains("a/pom.xml"));
        assertFalse(diff.buildAffecting().contains("repositories-backup.xml"));
        assertFalse(diff.buildAffecting().contains("manipulation.json"));
        assertFalse(diff.buildAffecting().contains("alignmentReport.json"));
        assertFalse(diff.buildAffecting().contains("gme-repos.gradle"));

        assertTrue(diff.metadata().contains("repositories-backup.xml"));
        assertTrue(diff.metadata().contains("manipulation.json"));
        assertTrue(diff.metadata().contains("alignmentReport.json"));
        assertTrue(diff.metadata().contains("gradle/gme-repos.gradle"));
        assertFalse(diff.metadata().contains("a/pom.xml"));
    }

    @Test
    void shouldReconstructOriginalContentWhenConcatenated() {
        String unifiedDiff = REPOSITORIES_BACKUP_SECTION + "\n" + POM_SECTION + "\n";

        AlignmentDiff diff = AlignmentDiff.split(unifiedDiff);

        // The build-affecting part comes first here only because that is where pom.xml landed; recombining the two
        // parts must still yield every original line.
        String recombined = diff.metadata() + diff.buildAffecting();
        assertTrue(recombined.contains("+<version>1.0.0.redhat-00001</version>"));
        assertTrue(recombined.contains("+<settings/>"));
    }
}
