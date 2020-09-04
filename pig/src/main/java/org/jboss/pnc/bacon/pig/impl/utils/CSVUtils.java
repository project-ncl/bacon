/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Harsh Madhani<harshmadhani@gmail.com> Date: 06-August-2020
 */
public class CSVUtils {
    public static Set<String> columnValues(String columnName, String filePath, char delimiter) throws IOException {
        try (Reader in = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.EXCEL.withDelimiter(delimiter).withFirstRecordAsHeader().parse(in)) {
            return parser.getRecords().stream().map(record -> record.get(columnName)).collect(Collectors.toSet());

        }
    }
}
