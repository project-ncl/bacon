package org.jboss.pnc.bacon.pig.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 2/22/19
 */
public class CollectionUtils {
    public static <T> Set<T> subtractSet(Set<T> minuend, Set<T> subtrahend) {
        Set<T> resultSet = new HashSet<>(minuend);
        resultSet.removeAll(subtrahend);
        return resultSet;
    }
}
