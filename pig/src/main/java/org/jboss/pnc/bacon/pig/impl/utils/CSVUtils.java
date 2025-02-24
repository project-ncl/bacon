package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

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
