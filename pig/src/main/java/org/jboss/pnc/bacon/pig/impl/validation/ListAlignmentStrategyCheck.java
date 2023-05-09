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
 * Validator annotation for Build config
 */
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ListAlignmentStrategyValidator.class)
public @interface ListAlignmentStrategyCheck {

    String message() default "Alignment strategy dependencyOverrides are not unique";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
