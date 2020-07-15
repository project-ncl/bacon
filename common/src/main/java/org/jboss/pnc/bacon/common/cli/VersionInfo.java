package org.jboss.pnc.bacon.common.cli;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Slf4j
@Getter
public class VersionInfo {

    private static final HashMap<String, VersionInfo> instance = new HashMap();

    private String revision;
    private String version;

    private VersionInfo() {
        String manifestName = JarFile.MANIFEST_NAME;
        InputStream manifestStream = getClass().getClassLoader().getResourceAsStream(manifestName);
        try {
            Manifest manifest = new Manifest(manifestStream);
            Attributes attributes = manifest.getMainAttributes();
            revision = attributes.getValue("Implementation-SCM-Revision");
            if (revision == null) {
                log.warn("Incomplete manifest file, missing revision property.");
            }
            version = attributes.getValue("Implementation-Version");
            if (version == null) {
                log.warn("Incomplete manifest file, missing version.");
            }
        } catch (IOException e) {
            log.warn("Cannot read manifest file. {}", e.getMessage());
        }
    }

    public static VersionInfo instance() {
        return instance.computeIfAbsent("INSTANCE", k -> new VersionInfo());
    }

}
