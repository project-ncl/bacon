package org.jboss.bacon.experimental.impl.dependencies;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GACTV;

class GACTVParser {
    public static ArtifactCoords parse(String str) {
        String[] split = str.split(":");
        String[] parts = new String[5];
        switch (split.length) {
            case 1:
            case 2:
                throw new IllegalArgumentException("GACTV \"" + str + "\" should have at least 3 parts separated by :");
            case 3:
                parts[2] = "*";
                parts[3] = "*";
                parts[4] = split[2];
                break;
            case 4:
                parts[2] = split[2];
                parts[3] = "*";
                parts[4] = split[3];
                break;
            case 5:
                parts[2] = split[2];
                parts[3] = split[3];
                parts[4] = split[4];
                break;
            default:
                throw new IllegalArgumentException("GACTV \"" + str + "\" should have at most 5 parts separated by :");
        }
        parts[0] = split[0];
        parts[1] = split[1];
        for (String part : parts) {
            if (part.isBlank()) {
                throw new IllegalArgumentException("GACTV \"" + str + "\" has a blank part.");
            }
        }
        return new GACTV(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }
}
