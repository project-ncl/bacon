package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/23/20
 */
public class QuarkusExtensions {
    private List<QuarkusExtension> extensions;

    @java.lang.SuppressWarnings("all")
    public QuarkusExtensions() {
    }

    @java.lang.SuppressWarnings("all")
    public List<QuarkusExtension> getExtensions() {
        return this.extensions;
    }

    @java.lang.SuppressWarnings("all")
    public void setExtensions(final List<QuarkusExtension> extensions) {
        this.extensions = extensions;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof QuarkusExtensions))
            return false;
        final QuarkusExtensions other = (QuarkusExtensions) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$extensions = this.getExtensions();
        final java.lang.Object other$extensions = other.getExtensions();
        if (this$extensions == null ? other$extensions != null : !this$extensions.equals(other$extensions))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof QuarkusExtensions;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $extensions = this.getExtensions();
        result = result * PRIME + ($extensions == null ? 43 : $extensions.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "QuarkusExtensions(extensions=" + this.getExtensions() + ")";
    }
}
