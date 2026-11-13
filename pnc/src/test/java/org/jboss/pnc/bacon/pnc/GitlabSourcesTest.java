package org.jboss.pnc.bacon.pnc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class GitlabSourcesTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BASE = "65f00aeed880a0b8a34687b41faeee12459730a2";
    private static final String HEAD = "4e2c2cf0ee5033d0a5ea1ceb212715fbb7ffdff7";

    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final List<String> tokens = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void archiveCandidates_useApiV4WithEncodedProjectPath() {
        List<BuildSourcesDownloader.SourceArchiveCandidate> candidates = BuildSourcesDownloader.sourceArchiveCandidates(
                "https://gitlab.cee.redhat.com/pnc-workspace/lightwell-builds-github.com-apache-zookeeper.git",
                List.of(HEAD, "cumulative/3.8.3/pipeline-19164783"));

        assertThat(candidates).extracting(BuildSourcesDownloader.SourceArchiveCandidate::uri)
                .containsExactly(
                        URI.create(
                                "https://gitlab.cee.redhat.com/api/v4/projects/"
                                        + "pnc-workspace%2Flightwell-builds-github.com-apache-zookeeper"
                                        + "/repository/archive.tar.gz?sha=" + HEAD),
                        URI.create(
                                "https://gitlab.cee.redhat.com/api/v4/projects/"
                                        + "pnc-workspace%2Flightwell-builds-github.com-apache-zookeeper"
                                        + "/repository/archive.tar.gz?sha=cumulative%2F3.8.3%2Fpipeline-19164783"));
    }

    @Test
    void archiveCandidates_nestedGroupsAndScpUrl() {
        assertThat(
                BuildSourcesDownloader
                        .sourceArchiveCandidates("git@gitlab.cee.redhat.com:pnc-workspace/airlift/airbase.git", HEAD))
                .extracting(BuildSourcesDownloader.SourceArchiveCandidate::uri)
                .containsExactly(
                        URI.create(
                                "https://gitlab.cee.redhat.com/api/v4/projects/pnc-workspace%2Fairlift%2Fairbase"
                                        + "/repository/archive.tar.gz?sha=" + HEAD));
    }

    @Test
    void githubCandidatesAreUnchanged() {
        assertThat(
                BuildSourcesDownloader.sourceArchiveCandidates("git@github.ibm.com:pnc-prod/airlift-airbase.git", HEAD))
                .extracting(c -> c.uri().toString())
                .first()
                .isEqualTo("https://github.ibm.com/api/v3/repos/pnc-prod/airlift-airbase/tarball/" + HEAD);
    }

    @Test
    void credentials() {
        assertThat(GitlabSources.credentials("t")).extracting(c -> c.header + ": " + c.value)
                .containsExactly("PRIVATE-TOKEN: t", "Authorization: Bearer t", "null: null");
        assertThat(GitlabSources.credentials(" ")).extracting(c -> c.name).containsExactly("no token");
    }

    @Test
    void toUnifiedDiff_rebuildsGitHeaders() throws IOException {
        JsonNode diffs = JSON.readTree(
                "["
                        + "{\"old_path\":\"pom.xml\",\"new_path\":\"pom.xml\",\"a_mode\":\"100644\",\"b_mode\":\"100644\","
                        + "\"diff\":\"@@ -1 +1 @@\\n-<version>3.8.3</version>\\n+<version>3.8.3.rhlw-00014</version>\\n\"},"
                        + "{\"old_path\":\"new.txt\",\"new_path\":\"new.txt\",\"a_mode\":\"0\",\"b_mode\":\"100644\","
                        + "\"new_file\":true,\"diff\":\"@@ -0,0 +1 @@\\n+hello\"},"
                        + "{\"old_path\":\"gone.txt\",\"new_path\":\"gone.txt\",\"a_mode\":\"100644\",\"b_mode\":\"0\","
                        + "\"deleted_file\":true,\"diff\":\"@@ -1 +0,0 @@\\n-bye\\n\"},"
                        + "{\"old_path\":\"a.txt\",\"new_path\":\"b.txt\",\"a_mode\":\"100644\",\"b_mode\":\"100644\","
                        + "\"renamed_file\":true,\"diff\":\"\"},"
                        + "{\"old_path\":\"run.sh\",\"new_path\":\"run.sh\",\"a_mode\":\"100644\",\"b_mode\":\"100755\","
                        + "\"diff\":\"\"},"
                        + "{\"old_path\":\"logo.png\",\"new_path\":\"logo.png\",\"a_mode\":\"100644\",\"b_mode\":\"100644\","
                        + "\"diff\":\"\"}"
                        + "]");

        GitlabSources.UnifiedDiff diff = GitlabSources.toUnifiedDiff(diffs);

        assertThat(diff.content).isEqualTo(
                "diff --git a/pom.xml b/pom.xml\n" + "--- a/pom.xml\n" + "+++ b/pom.xml\n" + "@@ -1 +1 @@\n"
                        + "-<version>3.8.3</version>\n" + "+<version>3.8.3.rhlw-00014</version>\n"
                        + "diff --git a/new.txt b/new.txt\n" + "new file mode 100644\n" + "--- /dev/null\n"
                        + "+++ b/new.txt\n" + "@@ -0,0 +1 @@\n" + "+hello\n" + "diff --git a/gone.txt b/gone.txt\n"
                        + "deleted file mode 100644\n" + "--- a/gone.txt\n" + "+++ /dev/null\n" + "@@ -1 +0,0 @@\n"
                        + "-bye\n" + "diff --git a/a.txt b/b.txt\n" + "rename from a.txt\n" + "rename to b.txt\n"
                        + "diff --git a/run.sh b/run.sh\n" + "old mode 100644\n" + "new mode 100755\n"
                        + "diff --git a/logo.png b/logo.png\n" + "Binary files a/logo.png and b/logo.png differ\n");
        assertThat(diff.incompleteFiles).containsExactly("logo.png");

        // the rebuilt diff is understood by the splitter
        AlignmentDiff split = AlignmentDiff
                .split(diff.content, List.of("3.8.3.rhlw-00014"), "org.apache.zookeeper:parent");
        assertThat(split.has(AlignmentDiff.Category.BUILD)).isTrue();
        assertThat(split.part(AlignmentDiff.Category.BUILD)).contains("+<version>3.8.3.rhlw-00014</version>");
    }

    @Test
    void toUnifiedDiff_emptyNewFileIsComplete_tooLargeNewFileIsNot() throws IOException {
        // as returned by gitlab.cee for PME's marker file and for a 280 KB generated source file
        JsonNode diffs = JSON.readTree(
                "["
                        + "{\"old_path\":\"target/pom-manip-ext-marker.txt\",\"new_path\":\"target/pom-manip-ext-marker.txt\","
                        + "\"a_mode\":\"0\",\"b_mode\":\"100644\",\"new_file\":true,\"too_large\":false,\"diff\":\"\"},"
                        + "{\"old_path\":\"SqlLexer.java\",\"new_path\":\"SqlLexer.java\",\"a_mode\":\"0\","
                        + "\"b_mode\":\"100644\",\"new_file\":true,\"too_large\":true,\"diff\":\"\"},"
                        + "{\"old_path\":\"gradle/wrapper.jar\",\"new_path\":\"gradle/wrapper.jar\",\"a_mode\":\"0\","
                        + "\"b_mode\":\"100644\",\"new_file\":true,"
                        + "\"diff\":\"Binary files /dev/null and b/gradle/wrapper.jar differ\\n\"}"
                        + "]");

        GitlabSources.UnifiedDiff diff = GitlabSources.toUnifiedDiff(diffs);

        assertThat(diff.content).isEqualTo(
                "diff --git a/target/pom-manip-ext-marker.txt b/target/pom-manip-ext-marker.txt\n"
                        + "new file mode 100644\n" + "diff --git a/SqlLexer.java b/SqlLexer.java\n"
                        + "new file mode 100644\n" + "Binary files /dev/null and b/SqlLexer.java differ\n"
                        + "diff --git a/gradle/wrapper.jar b/gradle/wrapper.jar\n" + "new file mode 100644\n"
                        + "Binary files /dev/null and b/gradle/wrapper.jar differ\n");
        assertThat(diff.incompleteFiles).containsExactly("SqlLexer.java", "gradle/wrapper.jar");
    }

    @Test
    void compare_usesMergeBaseAndStraightCompare() throws IOException {
        GitlabSources gitlab = gitlab(exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            if (path.endsWith("/repository/merge_base")) {
                send(exchange, 200, "{\"id\":\"" + BASE + "\"}");
            } else if (path.endsWith("/repository/compare")) {
                send(
                        exchange,
                        200,
                        "{\"diffs\":[{\"old_path\":\"pom.xml\",\"new_path\":\"pom.xml\",\"a_mode\":\"100644\","
                                + "\"b_mode\":\"100644\",\"diff\":\"@@ -1 +1 @@\\n-a\\n+b\\n\"}]}");
            } else {
                send(exchange, 404, "{}");
            }
        });
        GitlabSources.Credential credential = GitlabSources.credentials("secret").get(0);

        assertThat(gitlab.isAncestor(BASE, HEAD, credential)).isTrue();
        assertThat(gitlab.compare(BASE, HEAD, credential).content).contains("+b");
        assertThat(requests).containsExactly(
                "/api/v4/projects/pnc-workspace%2Fairlift%2Fairbase/repository/merge_base?refs%5B%5D=" + BASE
                        + "&refs%5B%5D="
                        + HEAD,
                "/api/v4/projects/pnc-workspace%2Fairlift%2Fairbase/repository/compare?from=" + BASE + "&to=" + HEAD
                        + "&straight=true");
        assertThat(tokens).containsOnly("secret");
    }

    @Test
    void isAncestor_falseForOtherMergeBase() throws IOException {
        GitlabSources gitlab = gitlab(exchange -> send(exchange, 200, "{\"id\":\"" + HEAD + "\"}"));

        assertThat(gitlab.isAncestor(BASE, HEAD, GitlabSources.credentials("t").get(0))).isFalse();
    }

    @Test
    void commitDiff_followsPages() throws IOException {
        StringBuilder page1 = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            page1.append(i == 0 ? "" : ",")
                    .append("{\"old_path\":\"f")
                    .append(i)
                    .append("\",\"new_path\":\"f")
                    .append(i)
                    .append("\",\"a_mode\":\"100644\",\"b_mode\":\"100644\",\"diff\":\"@@ -1 +1 @@\\n-a\\n+b\\n\"}");
        }
        page1.append(']');
        GitlabSources gitlab = gitlab(exchange -> {
            if (!exchange.getRequestURI().getPath().endsWith("/diff")) {
                // the commit itself, for its parent
                send(exchange, 200, "{\"id\":\"" + HEAD + "\",\"parent_ids\":[\"" + BASE + "\"]}");
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            send(
                    exchange,
                    200,
                    query.endsWith("&page=1") ? page1.toString()
                            : "[{\"old_path\":\"last\",\"new_path\":\"last\",\"a_mode\":\"100644\","
                                    + "\"b_mode\":\"100644\",\"diff\":\"@@ -1 +1 @@\\n-a\\n+b\\n\"}]");
        });

        GitlabSources.UnifiedDiff diff = gitlab.commitDiff(HEAD, GitlabSources.credentials("t").get(0));

        assertThat(diff.content).contains("diff --git a/f0 b/f0").contains("diff --git a/f99 b/f99").contains("a/last");
        assertThat(requests).hasSize(3);
    }

    @Test
    void leftOutFiles_areReadWithoutTokenWhenTheTokenIsRejected() throws IOException {
        // like gitlab.cee with a token of another project: compare answers, raw files are refused (401)
        GitlabSources gitlab = gitlab(exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            boolean withToken = exchange.getRequestHeaders().containsKey("PRIVATE-TOKEN");
            if (path.endsWith("/repository/compare")) {
                send(
                        exchange,
                        200,
                        "{\"diffs\":[{\"old_path\":\"lib/a.jar\",\"new_path\":\"lib/a.jar\",\"a_mode\":\"0\","
                                + "\"b_mode\":\"100644\",\"new_file\":true,"
                                + "\"diff\":\"Binary files /dev/null and b/lib/a.jar differ\\n\"}]}");
            } else if (path.contains("/repository/files/") && !withToken) {
                send(exchange, 200, "\u0000jar");
            } else {
                send(exchange, 401, "{\"message\":\"401 Unauthorized\"}");
            }
        });
        GitlabSources.Credential token = GitlabSources.credentials("other-project-token").get(0);

        GitlabSources.UnifiedDiff diff = gitlab.compare(BASE, HEAD, token);

        assertThat(diff.incompleteFiles).isEmpty();
        assertThat(diff.content).contains("GIT binary patch");
    }

    @Test
    void error_hasStatus() throws IOException {
        GitlabSources gitlab = gitlab(exchange -> send(exchange, 401, "{\"message\":\"401 Unauthorized\"}"));

        assertThatThrownBy(() -> gitlab.compare(BASE, HEAD, GitlabSources.credentials("bad").get(0)))
                .isInstanceOfSatisfying(
                        GitlabSources.GitlabApiException.class,
                        e -> assertThat(e.status).isEqualTo(401))
                .hasMessageContaining("401 Unauthorized");
    }

    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private GitlabSources gitlab(Handler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(
                    exchange.getRequestURI().getRawPath()
                            + (exchange.getRequestURI().getRawQuery() != null
                                    ? "?" + exchange.getRequestURI().getRawQuery()
                                    : ""));
            tokens.add(exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN"));
            handler.handle(exchange);
        });
        server.start();
        return new GitlabSources(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "pnc-workspace/airlift/airbase",
                HttpClient.newHttpClient(),
                UrlRewrites.none());
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
