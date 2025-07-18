package org.jboss.pnc.bacon.pig.impl.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FileCacheTest {

    @Test
    void testBasicFunctionality() {

        String textToCache = "hello-world";

        try {
            Path tempCacheFile = Files.createTempFile("bacon-test-", "-file-cache");
            Files.delete(tempCacheFile); // if the file exists but is empty, MapDB complains

            FileCache fileCache = new FileCache(tempCacheFile);

            Path tempFileToCache = Files.createTempFile("bacon-test-file-to-cache", "-file-cache");
            Files.writeString(tempFileToCache, textToCache);

            fileCache.put("test", tempFileToCache.toFile());

            Path tempFileFromCache = Files.createTempFile("bacon-test-file-from-cache", "-file-cache");
            boolean fetched = fileCache.copyTo("test", tempFileFromCache.toFile());
            assertTrue(fetched);

            String retrievedValue = Files.readString(tempFileFromCache);
            assertEquals(textToCache, retrievedValue);

            assertFalse(fileCache.copyTo("non-existent-key", tempFileFromCache.toFile()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
