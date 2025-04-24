package org.jboss.pnc.bacon.licenses.xml;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Offline License Mirror implementation
 *
 * The mirror is in the 'src/main/resources' folder, in path OFFLINE_LICENSE_MIRROR_RESOURCE_FOLDER
 * The structure of the mirror is: server/path1/path2/license.html
 * It tries to mimic the exact same path as the url, minus the scheme part (http)
 */
public class OfflineLicenseMirror {

    /**
     * Folder in the src/main/resources folder where to find the offline mirror for licenses
     */
    public static final String OFFLINE_LICENSE_MIRROR_RESOURCE_FOLDER = "offline-license-mirror";

    public static Optional<InputStream> find(String url) {

        // try to parse the url to extract values
        URI uri = URI.create(url);

        Path resourcesPathOfLicense = Paths.get(OFFLINE_LICENSE_MIRROR_RESOURCE_FOLDER, uri.getAuthority());

        // consider each path in the url as a sub-folder
        for (String subPath : uri.getPath().split("/")) {
            resourcesPathOfLicense = resourcesPathOfLicense.resolve(subPath);
        }

        InputStream input = OfflineLicenseMirror.class.getClassLoader()
                .getResourceAsStream(resourcesPathOfLicense.toString());
        return Optional.ofNullable(input);
    }
}
