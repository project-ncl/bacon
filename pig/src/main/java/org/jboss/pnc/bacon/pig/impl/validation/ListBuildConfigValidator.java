package org.jboss.pnc.bacon.pig.impl.validation;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.enums.BuildType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation implementation for BuildConfig
 */
@Slf4j
public class ListBuildConfigValidator implements ConstraintValidator<ListBuildConfigCheck, List<BuildConfig>> {

    private ListBuildConfigCheck constraintAnnotation;
    private Validator validator;

    @Override
    public boolean isValid(List<BuildConfig> values, ConstraintValidatorContext context) {

        List<String> errors = new ArrayList<>();

        for (BuildConfig value : values) {

            if (value.getExternalScmUrl() != null) {
                log.warn(
                        "Build config " + value.getName()
                                + " is using deprecated externalScmUrl property that is not used when creating pnc configurations anymore, use scmUrl instead.");
            }

            if (value == null) {
                // we do nothing if value is null?
                errors.add("is null");
            } else if (value.getScmUrl() == null) {
                errors.add(
                        "Build config " + value.getName() + " has scmUrl not specified. Specify the key with a value");
            } else if (value.getRawEnvironmentId() == null && value.getSystemImageId() == null
                    && value.getEnvironmentName() == null) {
                errors.add(
                        "Build config " + value.getName()
                                + " has neither environmentId, environmentName nor systemImageId specified. Specify one of the keys with a value");
            } else {
                try {
                    BuildType.valueOf(value.getBuildType());
                } catch (IllegalArgumentException e) {
                    errors.add("Build config " + value.getName() + " has wrong buildType");
                }
            }
        }

        if (errors.isEmpty()) {
            // everything is good!
            return true;
        } else {
            // Set the error message for the validation violation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(String.join("\n", errors)).addConstraintViolation();
            return false;
        }
    }

    @Override
    public void initialize(ListBuildConfigCheck constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
}
