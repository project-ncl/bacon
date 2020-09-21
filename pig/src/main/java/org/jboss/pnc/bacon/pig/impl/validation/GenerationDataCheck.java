package org.jboss.pnc.bacon.pig.impl.validation;

import javax.validation.Constraint;
import javax.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validator annotation for GenerationData
 */
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = GenerationDataValidator.class)
public @interface GenerationDataCheck {

    String message() default "Generation Data validation fail";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
