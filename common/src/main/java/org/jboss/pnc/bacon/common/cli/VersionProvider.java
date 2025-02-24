package org.jboss.pnc.bacon.common.cli;

import org.commonjava.maven.ext.common.util.ManifestUtils;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {
    /**
     * Returns version information for a command.
     *
     * @return version information (each string in the array is displayed on a separate line)
     * @throws Exception an exception detailing what went wrong when obtaining version information
     */
    @Override
    public String[] getVersion() throws Exception {
        return new String[] { "Bacon version " + ManifestUtils.getManifestInformation(VersionProvider.class) };
    }
}
