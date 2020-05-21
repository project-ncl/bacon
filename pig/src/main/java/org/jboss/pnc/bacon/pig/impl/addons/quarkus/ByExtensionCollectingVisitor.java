package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
public class ByExtensionCollectingVisitor extends SimpleFileVisitor<Path> {
    private final List<Path> filePaths = new ArrayList<>();
    private final String extension;

    public ByExtensionCollectingVisitor(String extension) {
        this.extension = extension;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (Files.isRegularFile(file) && file.toString().endsWith(extension)) {
            filePaths.add(file.toAbsolutePath());
        }
        return FileVisitResult.CONTINUE;
    }

    public List<Path> getFilePaths() {
        return filePaths;
    }
}
