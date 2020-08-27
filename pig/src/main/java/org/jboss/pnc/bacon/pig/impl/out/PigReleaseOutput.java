package org.jboss.pnc.bacon.pig.impl.out;

import lombok.Data;

@Data
public class PigReleaseOutput {
    private String releaseDirName;
    private String releasePath;
    private String nvrListFile;

    public PigReleaseOutput(String releaseDirName, String releasePath, String nvrListFile) {
        this.releaseDirName = releaseDirName;
        this.releasePath = releasePath;
        this.nvrListFile = nvrListFile;
    }
}
