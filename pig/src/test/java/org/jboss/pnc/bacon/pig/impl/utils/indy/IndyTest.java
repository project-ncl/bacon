package org.jboss.pnc.bacon.pig.impl.utils.indy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IndyTest {

    @BeforeAll
    static void setup() throws IOException {
        File file = new File(IndyTest.class.getClassLoader().getResource("config.yaml").getFile());
        Config.configure(file.getParent(), "config.yaml", "default");
        Config.initialize();
    }

    @Test
    void getIndyUrl() {
        assertNotNull(Indy.getIndyUrl());
        assertTrue(Indy.getIndyUrl().contains("http://indy.com/api/content/maven/group/static"));
    }

    @Test
    void getIndyTempUrl() {
        assertNotNull(Indy.getIndyTempUrl());
        assertTrue(Indy.getIndyTempUrl().contains("http://indy.com/api/content/maven/hosted/temporary-builds"));
    }
}
