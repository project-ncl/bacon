package org.jboss.pnc.bacon.pig.impl.addons.releasehandover;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

// TODO validate required
public record ReleaseHandover(
        String version, // TODO validate "^[0-9]+\\.[0-9]+\\.[0-9]+$"
        String type,
        Metadata metadata,
        List<Signoffs> signoffs,
        Product product,
        List<Advisory> advisory,
        List<Deliverable> deliverables,
        String releaseNotes) {

    public record Metadata(String owner, Instant producedAt) {
    }

    public record Signoffs(String actor, List<String> refs) {
        // TODO validate min one ref
        // TODO validate ref uri
    }

    public record Product(
            String id,
            String name,
            String version,
            String patchId,
            ProductReleaseType release,
            Instant releaseDate) {
    }

    public enum ProductReleaseType {
        GA,
        BETA,
        EA
    }

    @JsonDeserialize(using = AdvisoryDeserializer.class)
    public interface Advisory {
        @JsonIgnore
        default boolean isRHxA() {
            return this.getClass().equals(AdvisoryRHxA.class);
        }
    }

    public record AdvisoryRHxA(
            AdvisoryRHxAType type,
            Map<String, String> issues,
            Map<String, String> cves,
            List<String> references, // TODO validate uri
            String synopsis,
            String topic,
            String description,
            String solution) implements Advisory {
    }

    public record AdvisoryContent(String id, String contentType) implements Advisory {
    }

    public enum AdvisoryRHxAType {
        RHBA,
        RHEA,
        RHSA
    }

    public record Deliverable(DeliverableType type, List<Ref> refs) {
    }

    public record Ref(
            String url,
            String checksum, // TODO validate "^(md5|sha1|sha256|sha384|sha512):[a-fA-F0-9]+$"
            String filename,
            String description,
            List<Destination> destinations) {
    }

    public enum DeliverableType {
        MAVEN_REPOSIRTOY,
        LICENSE,
        JAVADOC,
        SOURCES,
        MISC
    }

    public enum Destination {
        CSP,
        MRRC
    }

    public static class AdvisoryDeserializer extends StdDeserializer<Advisory> {

        public AdvisoryDeserializer() {
            this(null);
        }

        public AdvisoryDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Advisory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            ObjectMapper om = (ObjectMapper) p.getCodec();
            JsonNode node = om.readTree(p);
            if (node.has("type")) {
                return new AdvisoryRHxA(
                        om.convertValue(node.get("type"), AdvisoryRHxAType.class),
                        om.convertValue(node.get("issues"), new TypeReference<Map<String, String>>() {
                        }),
                        om.convertValue(node.get("cves"), new TypeReference<Map<String, String>>() {
                        }),
                        om.convertValue(node.get("references"), new TypeReference<List<String>>() {
                        }),
                        node.get("synopsis").asText(),
                        node.get("topic").asText(),
                        node.get("description").asText(),
                        node.get("solution").asText());
            } else {
                return new AdvisoryContent(
                        node.get("id").asText(),
                        node.get("contentType").asText());
            }
        }
    }

    public static class AdvisorySerializer extends StdSerializer<Advisory> {

        public AdvisorySerializer() {
            this(null);
        }

        public AdvisorySerializer(Class<Advisory> vc) {
            super(vc);
        }

        @Override
        public void serialize(Advisory value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            if (value.isRHxA()) {
                AdvisoryRHxA ar = (AdvisoryRHxA) value;
                gen.writeObjectField("type", ar.type());
                gen.writeObjectField("issues", ar.issues());
                gen.writeObjectField("cves", ar.cves());
                gen.writeObjectField("references", ar.references());
                gen.writeObjectField("synopsis", ar.synopsis());
                gen.writeObjectField("topic", ar.topic());
                gen.writeObjectField("description", ar.description());
                gen.writeObjectField("solution", ar.solution());
            } else {
                AdvisoryContent ac = (AdvisoryContent) value;
                gen.writeObjectField("id", ac.id());
                gen.writeObjectField("contentType", ac.contentType());
            }
            gen.writeEndObject();
        }
    }
}
