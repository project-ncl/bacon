/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.utils;

import org.jboss.pnc.client.RemoteCollection;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 28/06/2019
 */
public class PncClientUtils {
    public static <T> List<T> toList(RemoteCollection<T> collection) {
        return toStream(collection).collect(Collectors.toList());
    }

    public static <T> Stream<T> toStream(RemoteCollection<T> collection) {
        return StreamSupport.stream(collection.spliterator(), false);
    }

    public static Optional<String> findByNameQuery(String name) {
        return query("name==%s", name);
    }

    public static Optional<String> query(String format, Object... values) {
        return Optional.of(String.format(format, values));
    }

    public static <T> Optional<T> maybeSingle(RemoteCollection<T> collection) {
        List<T> list = toList(collection);
        switch (list.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(list.get(0));
            default:
                throw new RuntimeException("Expecting single result got " + list.size());
        }
    }

    private PncClientUtils() {
    }
}
