package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;

/**
 * @author Harsh Madhani<harshmadhani@gmail.com> Date: 06-August-2020
 */
public class CSVUtils {
    public static Set<String> columnValues(String columnName, String filePath, char delimiter) throws IOException {
        Reader in = new FileReader(filePath);
        return CSVFormat.EXCEL.withDelimiter(delimiter)
                .withFirstRecordAsHeader()
                .parse(in)
                .getRecords()
                .stream()
                .map(record -> record.get(columnName))
                .collect(Collectors.toSet());
    }
}
