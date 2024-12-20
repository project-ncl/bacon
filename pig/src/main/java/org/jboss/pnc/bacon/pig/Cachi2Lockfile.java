package org.jboss.pnc.bacon.pig;

import org.jboss.pnc.bacon.pig.impl.addons.cachi2.Cachi2LockfileGenerator;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "cachi2lockfile",
        description = "Generates a Cachi2 lock file for a given Maven repository ZIP file.")
public class Cachi2Lockfile implements Callable<Integer> {

    @CommandLine.Parameters(description = "Comma-separated paths to Maven repositories (ZIPs or directories)")
    private List<File> repositories = List.of();

    @CommandLine.Option(
            names = "--output",
            description = "Target output file. If not provided, defaults to "
                    + Cachi2LockfileGenerator.DEFAULT_OUTPUT_FILENAME)
    private File output;

    @CommandLine.Option(
            names = "--maven-repository-url",
            description = "Maven repository URL to record in the generated lock file. If not provided, org.jboss.pnc.bacon.pig.impl.utils.indy.Indy.getIndyUrl() will be used as the default one")
    private String mavenRepoUrl;

    @CommandLine.Option(
            names = "--preferred-checksum-alg",
            description = "Preferred checksum algorithm to record in the generated lock file. If not provided, the strongest available SHA version will be used")
    private String preferredChecksumAlg;

    @Override
    public Integer call() {
        if (repositories.isEmpty()) {
            throw new IllegalArgumentException("Maven repository location was not provided");
        }
        var generator = Cachi2LockfileGenerator.newInstance();
        if (output != null) {
            generator.setOutputFile(output.toPath());
        }
        if (mavenRepoUrl != null) {
            generator.setDefaultMavenRepositoryUrl(mavenRepoUrl);
        }
        if (preferredChecksumAlg != null) {
            generator.setPreferredChecksumAlg(preferredChecksumAlg);
        }
        for (var path : repositories) {
            if (!path.exists()) {
                throw new IllegalArgumentException(path + " does not exist");
            }
            generator.addMavenRepository(path.toPath());
        }
        generator.generate();
        return 0;
    }
}
