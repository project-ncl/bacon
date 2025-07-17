package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCache {

    private static final Logger log = LoggerFactory.getLogger(FileCache.class);
    private static final int MAX_SIZE_GB = 5;

    private DB db;
    private ConcurrentMap<String, byte[]> cachedMap;

    public FileCache(Path cacheFile) {

        // if cache file parent folder doesn't exist, create it
        if (!Files.exists(cacheFile.getParent())) {
            try {
                Files.createDirectories(cacheFile.getParent());
            } catch (IOException e) {
                log.error("Unable to create directory {}", cacheFile.getParent(), e);
                throw new RuntimeException(e);
            }
        }

        db = DBMaker.fileDB(cacheFile.toString())
                .closeOnJvmShutdown()
                .make();

        cachedMap = db.hashMap("file-map", Serializer.STRING, Serializer.BYTE_ARRAY)
                .expireStoreSize(MAX_SIZE_GB * 1024L * 1024L) // max size in bytes,
                .expireAfterGet()
                .expireAfterCreate()
                .createOrOpen();
    }

    public void put(String key, File file) {
        try {
            cachedMap.put(key, Files.readAllBytes(file.toPath()));
            db.commit();
        } catch (IOException e) {
            log.warn("Error writing file {} for cache. Skipping!", file.getAbsolutePath(), e);
        }
    }

    public boolean copyTo(String key, File targetPath) {
        try {

            byte[] data = cachedMap.get(key);

            if (data == null) {
                // either not present in cache, or evicted
                return false;
            }
            Files.write(targetPath.toPath(), data);
            return true;
        } catch (IOException e) {
            log.warn("Error reading file {} from cache. Skipping!", targetPath.getAbsolutePath(), e);
            return false;
        }
    }

}
