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
    private static final int MAX_SIZE_CACHE_GB = 5;
    private static final int MAX_SIZE_INDIVIDUAL_FILE_CACHE_MB = 50;

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
                .expireStoreSize(MAX_SIZE_CACHE_GB * 1024L * 1024L * 1024L) // max size in bytes,
                .expireAfterGet()
                .expireAfterCreate()
                .createOrOpen();
    }

    public void put(String key, File file) {
        try {
            if (file.length() < MAX_SIZE_INDIVIDUAL_FILE_CACHE_MB * 1024L * 1024L) {
                // This is done to prevent OutOfMemory issues because readAllBytes load everything in memory
                cachedMap.put(key, Files.readAllBytes(file.toPath()));
                db.commit();
            } else {
                log.warn("File {} is too big for the cache ({}). Skipping!", file.getAbsolutePath(), file.length());
            }
        } catch (IOException e) {
            log.warn("Error writing file {} for cache. Skipping!", file.getAbsolutePath(), e);
        }
    }

    public void listContent() {
        cachedMap.keySet().forEach(System.out::println);
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
