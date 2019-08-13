package org.jboss.pnc.bacon.testcommon;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("remote-test")
@Test
public @interface RemoteTest {
}
