package org.jboss.pnc.bacon;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * Utility class to be used by each Picocli object in order to obtain the class
 * that is it needs to run
 */
public final class CDILauncher {

    private static final Weld weld = new Weld();
    private static final WeldContainer container = weld.initialize();

    private CDILauncher() {}

    public static <T>  T getEntrypointProvider(Class<T> entrypoint) {
        return container.select(entrypoint).get();
    }
}
