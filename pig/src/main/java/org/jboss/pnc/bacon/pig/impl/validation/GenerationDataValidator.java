package org.jboss.pnc.bacon.pig.impl.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jboss.pnc.bacon.pig.impl.config.GenerationData;

/**
 * Validation implementation for GenerationDataCheck
 */
public class GenerationDataValidator implements ConstraintValidator<GenerationDataCheck, GenerationData> {

    private GenerationDataCheck constraintAnnotation;

    /**
     * If the strategy is DOWNLOAD and the sourceBuild and/or sourceArtifact is null, it is not valid
     *
     * @param value
     * @param context
     * @return
     */
    @Override
    public boolean isValid(GenerationData value, ConstraintValidatorContext context) {

        // if error is not null, that means it's not valid!
        String error = null;

        if (value == null) {
            // we do nothing if value is null?
            error = null;
        } else if (value.getStrategy().toString().equals("DOWNLOAD")
                && (value.getSourceBuild() == null || value.getSourceArtifact() == null)) {
            error = "strategy is 'DOWNLOAD', but 'sourceBuild' and/or 'sourceArtifact' is null!";
        }

        if (error == null) {
            // everything is good!
            return true;
        } else {
            // Set the error message for the validation violation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
            return false;
        }
    }

    @Override
    public void initialize(GenerationDataCheck constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;

    }
}
