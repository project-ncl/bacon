package org.jboss.pnc.bacon;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * Utility class to be used by each Picocli object in order to obtain the class
 * that is it needs to run
 */
public final class CDILauncher {

    private CDILauncher() {}

    public static <T>  T getEntrypointProvider(Class<T> entrypoint) {
        final Weld weld = new Weld();
        final WeldContainer container = weld.initialize();
        return container.select(entrypoint).get();
    }

}
