package org.jboss.pnc.bacon.pig;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.pig.impl.addons.sbom.MavenRepoCdxSbomGenerator;

import picocli.CommandLine;

@CommandLine.Command(
        name = "cdx-sbom",
        description = "Generates an SBOM in CycloneDX format for a given Maven repository ZIP file.")
public class CycloneDxSbom implements Callable<Integer> {

    @CommandLine.Parameters(description = "Comma-separated paths to Maven repositories (ZIPs or directories)")
    private List<File> repositories = List.of();

    @CommandLine.Option(
            names = "--output",
            description = "Target output file. If not provided, defaults to "
                    + MavenRepoCdxSbomGenerator.DEFAULT_OUTPUT_FILENAME)
    private File output;

    @CommandLine.Option(
            names = "--schema",
            description = "Desired CycloneDX schema version. If not provided, the default value will be the latest supported by the generator")
    private String schemaVersion;

    @Override
    public Integer call() throws Exception {
        if (repositories.isEmpty()) {
            throw new IllegalArgumentException("Maven repository location was not provided");
        }
        var generator = new MavenRepoCdxSbomGenerator();
        if (output != null) {
            generator.setOutputFile(output.toPath());
        }
        for (var path : repositories) {
            if (!path.exists()) {
                throw new IllegalArgumentException(path + " does not exist");
            }
            generator.addMavenRepository(path.toPath());
        }
        if (schemaVersion != null) {
            generator.setSchemaVersion(schemaVersion);
        }
        generator.generate();
        return 0;
    }
}
