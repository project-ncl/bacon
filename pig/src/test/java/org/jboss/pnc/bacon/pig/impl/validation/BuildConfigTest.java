package org.jboss.pnc.bacon.pig.impl.validation;

import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.enums.BuildType;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class BuildConfigTest {

    static Validator validator;
    static EasyRandom easyRandom;

    BuildConfig buildConfig;

    @BeforeAll
    static void setupAll() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        easyRandom = new EasyRandom();
    }

    @BeforeEach
    void setup() {
        buildConfig = easyRandom.nextObject(BuildConfig.class);
        buildConfig.setBuildType(BuildType.MVN.toString());
        buildConfig.setScmUrl("http://example.com");
        buildConfig.setExternalScmUrl(null);
    }

    /**
     * Build config name validation: Name can contain only alpha-numeric characters, hyphens, underscores and periods
     * and cannot start with a hyphen
     */
    @Test
    void testVerifyNameValidationSuccess() {
        assertThat(checkBuildConfigNameValid(buildConfig, "abc")).isEmpty();
        assertThat(checkBuildConfigNameValid(buildConfig, "ja-b._c08")).isEmpty();
        assertThat(checkBuildConfigNameValid(buildConfig, ".ja-b._c")).isEmpty();
        assertThat(checkBuildConfigNameValid(buildConfig, "_.ja-b._c_")).isEmpty();
        assertThat(checkBuildConfigNameValid(buildConfig, "9jab._c-")).isEmpty();
    }

    /**
     * Build config name validation: Name can contain only alpha-numeric characters, hyphens, underscores and periods
     * and cannot start with a hyphen
     */
    @Test
    void testVerifyNameValidationFail() {
        assertThat(checkBuildConfigNameValid(buildConfig, "-abc")).isNotEmpty();
        assertThat(checkBuildConfigNameValid(buildConfig, "a:b:c")).isNotEmpty();
        assertThat(checkBuildConfigNameValid(buildConfig, ".a085bc_.-%")).isNotEmpty();
    }

    private Set<ConstraintViolation<BuildConfig>> checkBuildConfigNameValid(
            BuildConfig buildConfig,
            String buildConfigName) {
        buildConfig.setName(buildConfigName);
        return validator.validate(buildConfig);
    }
}
