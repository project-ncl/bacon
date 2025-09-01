package org.jboss.pnc.bacon.pig.impl.out;

public class PigBuildOutput {
    private String message;
    private PigRunOutput pigRunOutput;

    public PigBuildOutput(String message, PigRunOutput pigRunOutput) {
        this.message = message;
        this.pigRunOutput = pigRunOutput;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public PigRunOutput getPigRunOutput() {
        return this.pigRunOutput;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.SuppressWarnings("all")
    public void setPigRunOutput(final PigRunOutput pigRunOutput) {
        this.pigRunOutput = pigRunOutput;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PigBuildOutput))
            return false;
        final PigBuildOutput other = (PigBuildOutput) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message))
            return false;
        final java.lang.Object this$pigRunOutput = this.getPigRunOutput();
        final java.lang.Object other$pigRunOutput = other.getPigRunOutput();
        if (this$pigRunOutput == null ? other$pigRunOutput != null : !this$pigRunOutput.equals(other$pigRunOutput))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PigBuildOutput;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $pigRunOutput = this.getPigRunOutput();
        result = result * PRIME + ($pigRunOutput == null ? 43 : $pigRunOutput.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PigBuildOutput(message=" + this.getMessage() + ", pigRunOutput=" + this.getPigRunOutput() + ")";
    }
}
