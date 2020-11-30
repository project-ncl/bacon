package org.jboss.pnc.bacon.pig.impl.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which groups fields that relay the same value
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldGroup {
    /**
     * represents group identifier
     *
     * f.e. "environment" identifier represents all fields that can by themself represent an environment
     */
    String value();
}
