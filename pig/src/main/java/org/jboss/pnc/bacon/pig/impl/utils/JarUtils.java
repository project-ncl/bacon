package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;

public final class JarUtils {

    private JarUtils() {
    }

    public static String getJarLocation(Class<?> clazz) {
        String pathAsString = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        String path = Paths.get(pathAsString).getParent().toAbsolutePath().toString();
        try {
            return URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to decode the jar file path", e);
        }
    }

}
