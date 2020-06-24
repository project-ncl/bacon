package org.jboss.pnc.bacon.common.futures;

import java.util.concurrent.Future;

/**
 * Stupid class to hold future static method helpers
 */
public class FutureUtils {

    /**
     * Print dots every waitSeconds while the future is not yet done. It provides a way for users to feel the program is
     * doing "something"
     *
     * @param future future to monitor
     * @param waitSeconds how many seconds to wait to check the status
     */
    public static void printDotWhileFutureIsInProgress(Future future, int waitSeconds) {

        while (!future.isDone()) {
            try {
                System.out.print(".");
                Thread.sleep(waitSeconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
