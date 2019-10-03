package org.jboss.pnc.bacon.common;

public class ObjectHelper {

    public static void executeIfNotNull(Object value, Runnable run) {
        if (value != null) {
            run.run();
        }
    }
}
