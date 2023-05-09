package org.jboss.pnc.bacon.pig.impl.validation;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.pig.impl.config.StrategyConfig;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ListAlignmentStrategyValidator
        implements ConstraintValidator<ListAlignmentStrategyCheck, List<StrategyConfig>> {

    @Override
    public boolean isValid(List<StrategyConfig> values, ConstraintValidatorContext context) {

        if (!values.isEmpty()) {
            Set<String> uniqueChecker = new HashSet<>();
            for (StrategyConfig value : values) {
                String override = value.getDependencyOverride();
                if (uniqueChecker.contains(override)) {
                    if (override == null) {
                        context.buildConstraintViolationWithTemplate(
                                "Global override with empty/null value is mentioned more than once.")
                                .addPropertyNode("dependencyOverride")
                                .addConstraintViolation();
                    } else {
                        context.buildConstraintViolationWithTemplate(
                                "Override '" + override + "' is mentioned more than once.")
                                .addPropertyNode("dependencyOverride")
                                .addConstraintViolation();
                    }
                    return false;
                }
                uniqueChecker.add(override);
            }
        }
        return true;
    }
}
