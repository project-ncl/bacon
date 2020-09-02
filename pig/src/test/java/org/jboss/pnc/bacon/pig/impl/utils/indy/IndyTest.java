package org.jboss.pnc.bacon.pig.impl.utils.indy;

import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(Indy.getIndyUrl().contains("http://indy.com/api/content"));
    }

    @Test
    void getIndyTempUrl() {
        assertNotNull(Indy.getIndyTempUrl());
        assertTrue(Indy.getIndyTempUrl().contains("http://indy.com/api/content"));
    }
}
