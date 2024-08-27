package org.jboss.pnc.bacon.pig.impl.addons.vertx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotYetAlignedFromDependencyTreeTest {

    @Test
    void testMatchingDependencyLine() {
        // repeated INFO line
        String line = "[INFO] [INFO] +- org.apache.kafka:connect-api:jar:3.6.1:compile";
        // whitespace at the beginning and end
        String line2 = " [INFO] +- org.apache.kafka:connect-api:jar:3.6.1:runtime    ";
        // shouldn't match that one since not top-level
        String nonMatchingLine = "[INFO] |  |  \\- com.fasterxml.jackson.core:jackson-core:jar:2.16.1.redhat-00003:compile";
        // shouldn't match since it is of type provided
        String nonMatchingLine2 = "[INFO] +- org.projectlombok:lombok:jar:1.18.32:provided";

        assertTrue(NotYetAlignedFromDependencyTree.matchingDependencyLine(line));
        assertTrue(NotYetAlignedFromDependencyTree.matchingDependencyLine(line2));
        assertFalse(NotYetAlignedFromDependencyTree.matchingDependencyLine(nonMatchingLine));
        assertFalse(NotYetAlignedFromDependencyTree.matchingDependencyLine(nonMatchingLine2));
    }
}
