package org.jboss.pnc.bacon.licenses.xml;

import java.io.InputStream;
import java.net.URI;
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

        // As a path separator for getResourceAsStream, we should always use a slash (“/”), not what the OS specific one uses.
        // Thanks to @aloubyansky for pointing this out
        String resourcesPathOfLicense = OFFLINE_LICENSE_MIRROR_RESOURCE_FOLDER + "/" + uri.getAuthority()
                + uri.getPath();

        InputStream input = OfflineLicenseMirror.class.getClassLoader()
                .getResourceAsStream(resourcesPathOfLicense);
        return Optional.ofNullable(input);
    }
}
