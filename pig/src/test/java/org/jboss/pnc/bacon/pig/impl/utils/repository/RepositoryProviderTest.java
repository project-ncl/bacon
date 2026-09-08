package org.jboss.pnc.bacon.pig.impl.utils.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RepositoryProviderTest {

    @BeforeAll
    static void setup() throws IOException {
        File file = new File(RepositoryProviderTest.class.getClassLoader().getResource("config.yaml").getFile());
        Config.configure(file.getParent(), "config.yaml", "default");
        Config.initialize();
    }

    @Test
    void getRepoUrl() {
        assertNotNull(RepositoryProvider.getRepoProviderUrl());
        assertTrue(
                RepositoryProvider.getRepoProviderUrl()
                        .contains("http://repo.com/artifactory/pnc-mvn-builds-imports"));
    }

    @Test
    void getRepoTempUrl() {
        assertNotNull(RepositoryProvider.getTempRepoProviderUrl());
        assertTrue(
                RepositoryProvider.getTempRepoProviderUrl()
                        .contains("http://repo.com/artifactory/pnc-mvn-ibm-temp-builds"));
    }
}
