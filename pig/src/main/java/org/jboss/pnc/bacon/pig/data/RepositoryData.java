package org.jboss.pnc.bacon.pig.data;

import lombok.Data;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/18/18
 */
@Data
public class RepositoryData {
    private Collection<GAV> gavs;
    private Collection<File> files;
    private Path repositoryPath;
}