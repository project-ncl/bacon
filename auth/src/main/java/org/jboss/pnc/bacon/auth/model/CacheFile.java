package org.jboss.pnc.bacon.auth.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.config.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@Slf4j
public class CacheFile {

    private Map<String, Credential> cachedData;
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public static void writeCredentialToCacheFile(
            String keycloakUrl,
            String realm,
            String username,
            Credential credential) {

        createConfigFolderIfAbsent();

        File file = new File(getCacheFile());

        log.debug("Writing credential to cache file {}", file);

        try {
            Map<String, Credential> data = Collections
                    .singletonMap(generateUsernameMd5(keycloakUrl, realm, username), credential);
            CacheFile cacheFile = new CacheFile();
            cacheFile.setCachedData(data);
            mapper.writeValue(file, cacheFile);
            setOwnerFilePermissions(getCacheFile());
        } catch (IOException e) {
            log.error("Error saving credential to file {}", file, e);
        }
    }

    public static Optional<Credential> getCredentialFromCacheFile(String keycloakUrl, String realm, String username) {
        String key = generateUsernameMd5(keycloakUrl, realm, username);

        if (!fileExists(getCacheFile())) {
            return Optional.empty();
        }

        try {
            CacheFile cacheFile = mapper.readValue(new File(getCacheFile()), CacheFile.class);
            Map<String, Credential> data = cacheFile.getCachedData();

            if (data != null) {
                return Optional.ofNullable(data.get(key));
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            log.error("Error getting credential", e);
            return Optional.empty();
        }
    }

    private static void createConfigFolderIfAbsent() {
        if (!fileExists(Config.getConfigLocation())) {
            String configFolder = Config.getConfigLocation();
            log.debug("Creating config folder {}", configFolder);
            new File(configFolder).mkdirs();
        }
    }

    private static void setOwnerFilePermissions(String path) {
        Path p = Paths.get(path);
        if (p.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Set<PosixFilePermission> set = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            try {
                Files.setPosixFilePermissions(p, set);
            } catch (IOException e) {
                log.error("Could not set file permissions for path {}", p, e);
            }
        }
    }

    private static String generateUsernameMd5(String keycloakUrl, String realm, String username) {
        return DigestUtils.md5Hex(keycloakUrl + ":" + realm + ":" + username);
    }

    private static boolean fileExists(String pathString) {
        Path path = Paths.get(pathString);
        return Files.exists(path);
    }

    private static String getCacheFile() {
        return Config.getConfigLocation() + File.separator + Constant.CACHE_FILE;
    }

}
