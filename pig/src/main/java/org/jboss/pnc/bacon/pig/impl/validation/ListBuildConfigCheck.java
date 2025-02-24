package org.jboss.pnc.bacon.pig.impl.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Validator annotation for Build config
 */
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ListBuildConfigValidator.class)
public @interface ListBuildConfigCheck {

    String message() default "Build Config validation fail";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
