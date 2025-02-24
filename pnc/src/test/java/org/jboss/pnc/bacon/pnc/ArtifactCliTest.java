package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ArtifactCliTest {

    @Test
    void testTransformIdentifierIfGAV() {
        String gav1 = "xom:xom:1.2.5";
        String identifier1 = "xom:xom:pom:1.2.5";

        String identifier2 = "toe:thumb:jar:2.0.0";

        String identifier3 = "hello:world:pom:1.2.3:test";

        assertEquals(ArtifactCli.transformIdentifierIfGAV(gav1), identifier1);
        assertEquals(ArtifactCli.transformIdentifierIfGAV(identifier2), identifier2);
        assertEquals(ArtifactCli.transformIdentifierIfGAV(identifier3), identifier3);
    }
}
