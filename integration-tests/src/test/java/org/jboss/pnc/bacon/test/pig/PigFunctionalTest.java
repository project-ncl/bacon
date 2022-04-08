package org.jboss.pnc.bacon.test.pig;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;
import org.jboss.pnc.bacon.config.Validate;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.test.CLIExecutor;
import org.jboss.pnc.bacon.test.TestType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import java.nio.file.Path;
import java.util.Collections;

@Tag(TestType.REAL_SERVICE_ONLY)
public abstract class PigFunctionalTest {
    static final String emptyNameBase1 = "michalszynkiewicz-et-%s";
    static final String emptyNameBase2 = "michalszynkiewicz-et2-%s";

    @BeforeAll
    static void initBaconConfig() {
        Config.configure(CLIExecutor.CONFIG_LOCATION.toString(), Constant.CONFIG_FILE_NAME, "default");
    }

    static String init(Path configDir, boolean clean, String releaseStorageUrl, Path targetDirectory) {
        String suffix = prepareSuffix();
        // todo release storage url mocking
        if (configDir == null) {
            throw new FatalException("You need to specify the configuration directory!");
        }
        // validate the PiG config
        PigConfig pig = Config.instance().getActiveProfile().getPig();
        if (pig == null) {
            throw new Validate.ConfigMissingException("Pig configuration missing");
        }
        pig.validate();

        PigContext.init(
                clean,
                configDir,
                targetDirectory.toAbsolutePath().toString(),
                releaseStorageUrl,
                Collections.emptyMap());
        return suffix;
    }

    static String prepareSuffix() {
        String suffix = RandomStringUtils.randomAlphanumeric(8);
        System.setProperty("suffix", suffix);
        return suffix;
    }
}
