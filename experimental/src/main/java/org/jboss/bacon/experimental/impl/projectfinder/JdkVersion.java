package org.jboss.bacon.experimental.impl.projectfinder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum JdkVersion {
    JDK_1_8("1.8", 8),
    JDK_11("11", 11),
    JDK_17("17", 17),
    JDK_21("21", 21),
    JDK_25("25", 25);

    private static final Logger log = LoggerFactory.getLogger(JdkVersion.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?");

    private final String label;
    private final int majorVersion;

    JdkVersion(String label, int majorVersion) {
        this.label = label;
        this.majorVersion = majorVersion;
    }

    public String getLabel() {
        return label;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public static JdkVersion fromVersionString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        Matcher matcher = VERSION_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return null;
        }
        int major = Integer.parseInt(matcher.group(1));
        if (major == 1 && matcher.group(2) != null) {
            major = Integer.parseInt(matcher.group(2));
        }
        JdkVersion result = fromMajorVersion(major);
        log.debug("JDK version string '{}' parsed as major={}, resolved to {}", raw, major, result);
        return result;
    }

    public static JdkVersion fromMajorVersion(int major) {
        if (major <= 0) {
            return null;
        }
        JdkVersion best = null;
        for (JdkVersion v : values()) {
            if (v.majorVersion <= major) {
                best = v;
            }
        }
        if (best == null) {
            best = values()[0];
        }
        return best;
    }
}
