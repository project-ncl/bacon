package org.jboss.bacon.experimental.impl.dependencies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Set;

import org.jboss.pnc.bacon.common.exception.FatalException;
import org.junit.jupiter.api.Test;

public class MavenCentralSearcherTest {

    private final MavenCentralSearcher searcher = new MavenCentralSearcher();

    @Test
    void parseValidResponse() throws IOException {
        String json = "{\"response\":{\"numFound\":3,\"docs\":["
                + "{\"g\":\"com.example\",\"a\":\"core\",\"v\":\"1.0\"},"
                + "{\"g\":\"com.example\",\"a\":\"utils\",\"v\":\"1.0\"},"
                + "{\"g\":\"com.example\",\"a\":\"api\",\"v\":\"1.0\"}"
                + "]}}";

        Set<String> artifacts = searcher.parseResponse(json);

        assertThat(artifacts).containsExactlyInAnyOrder("core", "utils", "api");
    }

    @Test
    void parseEmptyResponse() throws IOException {
        String json = "{\"response\":{\"numFound\":0,\"docs\":[]}}";

        Set<String> artifacts = searcher.parseResponse(json);

        assertThat(artifacts).isEmpty();
    }

    @Test
    void parseResponseWithMissingArtifactId() throws IOException {
        String json = "{\"response\":{\"numFound\":2,\"docs\":["
                + "{\"g\":\"com.example\",\"a\":\"core\",\"v\":\"1.0\"},"
                + "{\"g\":\"com.example\",\"v\":\"1.0\"}"
                + "]}}";

        Set<String> artifacts = searcher.parseResponse(json);

        assertThat(artifacts).containsExactly("core");
    }

    @Test
    void parseInvalidResponseFormat() {
        String json = "{\"unexpected\":\"format\"}";

        assertThatThrownBy(() -> searcher.parseResponse(json))
                .isInstanceOf(FatalException.class)
                .hasMessageContaining("Unexpected Maven Central search response format");
    }
}
