package org.jboss.pnc.bacon.pig.impl.out;

public class PigReleaseOutput {
    private String releaseDirName;
    private String releasePath;
    private String nvrListFile;

    public PigReleaseOutput(String releaseDirName, String releasePath, String nvrListFile) {
        this.releaseDirName = releaseDirName;
        this.releasePath = releasePath;
        this.nvrListFile = nvrListFile;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleaseDirName() {
        return this.releaseDirName;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleasePath() {
        return this.releasePath;
    }

    @java.lang.SuppressWarnings("all")
    public String getNvrListFile() {
        return this.nvrListFile;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleaseDirName(final String releaseDirName) {
        this.releaseDirName = releaseDirName;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleasePath(final String releasePath) {
        this.releasePath = releasePath;
    }

    @java.lang.SuppressWarnings("all")
    public void setNvrListFile(final String nvrListFile) {
        this.nvrListFile = nvrListFile;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PigReleaseOutput))
            return false;
        final PigReleaseOutput other = (PigReleaseOutput) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$releaseDirName = this.getReleaseDirName();
        final java.lang.Object other$releaseDirName = other.getReleaseDirName();
        if (this$releaseDirName == null ? other$releaseDirName != null
                : !this$releaseDirName.equals(other$releaseDirName))
            return false;
        final java.lang.Object this$releasePath = this.getReleasePath();
        final java.lang.Object other$releasePath = other.getReleasePath();
        if (this$releasePath == null ? other$releasePath != null : !this$releasePath.equals(other$releasePath))
            return false;
        final java.lang.Object this$nvrListFile = this.getNvrListFile();
        final java.lang.Object other$nvrListFile = other.getNvrListFile();
        if (this$nvrListFile == null ? other$nvrListFile != null : !this$nvrListFile.equals(other$nvrListFile))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PigReleaseOutput;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $releaseDirName = this.getReleaseDirName();
        result = result * PRIME + ($releaseDirName == null ? 43 : $releaseDirName.hashCode());
        final java.lang.Object $releasePath = this.getReleasePath();
        result = result * PRIME + ($releasePath == null ? 43 : $releasePath.hashCode());
        final java.lang.Object $nvrListFile = this.getNvrListFile();
        result = result * PRIME + ($nvrListFile == null ? 43 : $nvrListFile.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PigReleaseOutput(releaseDirName=" + this.getReleaseDirName() + ", releasePath=" + this.getReleasePath()
                + ", nvrListFile=" + this.getNvrListFile() + ")";
    }
}
