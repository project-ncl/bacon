package org.jboss.pnc.bacon.pnc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScmCredentialsTest {

    @Test
    void isGitlab_detectsTheProviderFromAnyUrlForm() {
        assertThat(ScmCredentials.isGitlab("git@gitlab.cee.redhat.com:pnc-workspace/airlift/airbase.git")).isTrue();
        assertThat(ScmCredentials.isGitlab("https://gitlab.cee.redhat.com/pnc-workspace/x.git")).isTrue();
        assertThat(ScmCredentials.isGitlab("ssh://git@gitlab.com/group/sub/x.git")).isTrue();

        assertThat(ScmCredentials.isGitlab("git@github.ibm.com:pnc-prod/airlift-airbase.git")).isFalse();
        assertThat(ScmCredentials.isGitlab("https://github.com/apache/zookeeper.git")).isFalse();
        assertThat(ScmCredentials.isGitlab(null)).isFalse();
        assertThat(ScmCredentials.isGitlab("not a url")).isFalse();
    }

    @Test
    void forGitClone_usesProviderSpecificUsername() {
        ScmCredentials.forGitClone("https://github.com/apache/zookeeper.git")
                .ifPresent(c -> assertThat(c.username()).isEqualTo("x-access-token"));
        ScmCredentials.forGitClone("https://gitlab.cee.redhat.com/pnc-workspace/x.git")
                .ifPresent(c -> assertThat(c.username()).isIn("oauth2", "gitlab-ci-token"));
    }
}
