/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/14/17
 */
public class RepoDescriptor {

    public static final String[] CHECKSUM_EXTENSIONS = { ".md5", ".sha1", "maven-metadata.xml" };
    public static final String MAVEN_REPOSITORY = "maven-repository/";

    public static Collection<GAV> listGavs(File m2RepoDirectory) {
        List<GAV> allGavs = listFiles(m2RepoDirectory).stream()
                .filter(f -> Stream.of(CHECKSUM_EXTENSIONS).noneMatch(ext -> f.getName().endsWith(ext)))
                .map(f -> GAV.fromFileName(f.getAbsolutePath(), MAVEN_REPOSITORY))
                .collect(Collectors.toList());
        Set<GAV> resultSet = new TreeSet<>(Comparator.comparing(GAV::toGav));
        resultSet.addAll(allGavs);
        return resultSet;
    }

    public static Collection<File> listFiles(File m2RepoDirectory) {
        return org.apache.commons.io.FileUtils.listFiles(m2RepoDirectory, null, true);
    }

    private RepoDescriptor() {
    }
}
