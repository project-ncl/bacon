package org.jboss.pnc.bacon.common;

import lombok.experimental.UtilityClass;

import java.io.File;

@UtilityClass
public class Constant {
    public final String DEFAULT_CONFIG_FOLDER = System.getProperty("user.home") + File.separator + ".config"
            + File.separator + "pnc-bacon";

    public final String CONFIG_FILE_NAME = "config.yaml";

    public final String CONFIG_ENV = "PNC_CONFIG_PATH";

    public final String PIG_CONTEXT_DIR = "PIG_CONTEXT_DIR";

    public final String CACHE_FILE = "saved-user.json";
}
