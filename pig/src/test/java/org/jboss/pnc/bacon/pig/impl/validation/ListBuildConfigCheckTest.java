package org.jboss.pnc.bacon.pig.impl.validation;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.enums.BuildType;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ListBuildConfigCheckTest {
    static Validator validator;
    static EasyRandom easyRandom;

    @BeforeAll
    static void setupAll() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        easyRandom = new EasyRandom();
    }

    @Test
    void checkForAtLeastOneScmUrlKeyToBeSpecified() {
        // when both fields are null
        BuildConfig buildConfig = easyRandom.nextObject(BuildConfig.class);
        buildConfig.setBuildType(BuildType.MVN.toString());
        buildConfig.setScmUrl(null);

        ListBuildConfigWrapper wrapper = new ListBuildConfigWrapper(Lists.newArrayList(buildConfig));
        Set<ConstraintViolation<ListBuildConfigWrapper>> violations = validator.validate(wrapper);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.stream().findFirst().get().getMessage()).contains(buildConfig.getName());

        // when only externalScmUrl null, no violation
        buildConfig.setScmUrl("http://example.com");
        violations = validator.validate(wrapper);

        // then
        assertThat(violations).isEmpty();

        // then
        assertThat(violations).isEmpty();
    }

    /**
     * TODO: write tests also for systemImageId. Can't really be done because 'getEnvironmentId' uses PNC Client right
     * now
     */
    @Test
    void checkForEnvironmentIdToBeSpecified() {

        BuildConfig buildConfig = easyRandom.nextObject(BuildConfig.class);
        buildConfig.setBuildType(BuildType.MVN.toString());
        ListBuildConfigWrapper wrapper = new ListBuildConfigWrapper(Lists.newArrayList(buildConfig));

        // when only environmentId is specified, no violations
        buildConfig.setEnvironmentId("5");
        buildConfig.setSystemImageId(null);
        Set<ConstraintViolation<ListBuildConfigWrapper>> violations = validator.validate(wrapper);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void checkIfBuildTypeIsValidated() {
        BuildConfig buildConfig = easyRandom.nextObject(BuildConfig.class);
        buildConfig.setBuildType("garbage");

        ListBuildConfigWrapper wrapper = new ListBuildConfigWrapper(Lists.newArrayList(buildConfig));
        Set<ConstraintViolation<ListBuildConfigWrapper>> violations = validator.validate(wrapper);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.stream().findFirst().get().getMessage()).contains(buildConfig.getName());

        // when all is good
        buildConfig.setBuildType(BuildType.GRADLE.toString());
        violations = validator.validate(wrapper);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void testIfItTestsAllBuildConfigs() {
        // only buildConfig1 is valid, buildConfig2 should fail
        BuildConfig buildConfig1 = easyRandom.nextObject(BuildConfig.class);
        BuildConfig buildConfig2 = easyRandom.nextObject(BuildConfig.class);

        buildConfig1.setBuildType(BuildType.NPM.toString());

        ListBuildConfigWrapper wrapper = new ListBuildConfigWrapper(Lists.newArrayList(buildConfig1, buildConfig2));
        Set<ConstraintViolation<ListBuildConfigWrapper>> violations = validator.validate(wrapper);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.stream().findFirst().get().getMessage()).contains(buildConfig2.getName());
    }

    @AllArgsConstructor
    class ListBuildConfigWrapper {
        @ListBuildConfigCheck
        List<BuildConfig> builds;

    }

}
