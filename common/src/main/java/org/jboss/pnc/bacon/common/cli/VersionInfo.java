package org.jboss.pnc.bacon.common.cli;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Slf4j
@Getter
public final class VersionInfo {
    private static final VersionInfo INSTANCE = new VersionInfo();

    private String version = "unknown";

    private String revision = "unknown";

    private String versionString = "unknown (unknown)";

    private VersionInfo() {
        ProtectionDomain protectionDomain = VersionInfo.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();

        if (codeSource != null) {
            URL location = codeSource.getLocation();

            try (JarInputStream jarInputStream = new JarInputStream(location.openStream())) {
                Manifest manifest = jarInputStream.getManifest();

                if (manifest == null) {
                    log.warn("Couldn't get manifest from {}", location);
                    return;
                }

                Attributes attributes = manifest.getMainAttributes();
                revision = attributes.getValue("Implementation-SCM-Revision");

                if (revision == null) {
                    log.warn("Incomplete manifest file, missing revision property");
                }

                version = attributes.getValue("Implementation-Version");

                if (version == null) {
                    log.warn("Incomplete manifest file, missing version property");
                }

                versionString = version + " (" + revision + ")";
            } catch (IOException e) {
                log.warn(
                        "Error opening manifest with protection domain {} and code source {}: {}",
                        protectionDomain,
                        codeSource,
                        e.getMessage());
            }
        } else {
            log.warn("Got null code source for protection domain {}", protectionDomain);
        }
    }

    public static VersionInfo instance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return INSTANCE.versionString;
    }
}
