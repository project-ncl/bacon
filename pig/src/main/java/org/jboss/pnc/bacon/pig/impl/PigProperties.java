package org.jboss.pnc.bacon.pig.impl;

import lombok.Getter;

import java.util.Properties;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com 2020-06-05
 */
@Getter
public class PigProperties {

    private boolean temporary;
    private boolean removeGeneratedM2Dups;
    private boolean skipBranchCheck;

    private PigProperties() {
    }

    private static PigProperties instance;

    public static synchronized void init(Properties properties) {
        instance = new PigProperties();
        instance.removeGeneratedM2Dups = Boolean.TRUE.toString()
                .equalsIgnoreCase(properties.getProperty("removeGeneratedM2Dups", "false"));
        instance.skipBranchCheck = Boolean.TRUE.toString()
                .equalsIgnoreCase(properties.getProperty("skipBranchCheck", "false"));
        instance.temporary = Boolean.TRUE.toString().equalsIgnoreCase(properties.getProperty("temporary", "false"));
    }

    public static synchronized PigProperties get() {
        if (instance == null) {
            init(System.getProperties());
        }
        return instance;
    }
}
