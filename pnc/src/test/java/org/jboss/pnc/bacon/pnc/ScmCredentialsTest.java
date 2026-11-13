package org.jboss.pnc.bacon.pnc;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;
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
        ConfigProfile profile = new ConfigProfile();
        profile.setGithubToken("github-secret");
        profile.setGitlabToken("gitlab-secret");
        Config config = new Config();
        config.setActiveProfile(profile);
        Config.setInstance(config);
        try {
            assertThat(ScmCredentials.forGitClone("https://github.com/apache/zookeeper.git")).contains(
                    new ScmCredentials.UsernamePassword("x-access-token", "github-secret"));
            assertThat(ScmCredentials.forGitClone("https://gitlab.cee.redhat.com/pnc-workspace/x.git"))
                    .contains(new ScmCredentials.UsernamePassword("oauth2", "gitlab-secret"));
        } finally {
            Config.setInstance(null);
        }
    }

    @Test
    void tokensAreNotInTheProfileToString() {
        ConfigProfile profile = new ConfigProfile();
        profile.setGithubToken("github-secret");
        profile.setGitlabToken("gitlab-secret");
        profile.setLdapUsernamePassword("user:ldap-secret");

        assertThat(profile.toString()).doesNotContain("github-secret", "gitlab-secret", "ldap-secret");
    }
}
