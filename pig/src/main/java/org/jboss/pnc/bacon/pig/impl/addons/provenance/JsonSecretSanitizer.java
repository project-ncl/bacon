/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.addons.provenance;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

public final class JsonSecretSanitizer {

    // Key-name patterns (case-insensitive)
    private static final List<Pattern> SENSITIVE_KEY_PATTERNS = List.of(
            Pattern.compile("(?i).*pass(word)?.*"),
            Pattern.compile("(?i).*secret.*"),
            Pattern.compile("(?i).*token.*"),
            Pattern.compile("(?i).*api[_-]?key.*"),
            Pattern.compile("(?i).*private[_-]?key.*"),
            Pattern.compile("(?i).*access[_-]?key.*"),
            Pattern.compile("(?i).*session.*"),
            Pattern.compile("(?i).*credential(s)?.*"),
            Pattern.compile("(?i).*bearer.*"),
            Pattern.compile("(?i).*authorization.*"),
            Pattern.compile("(?i).*refresh[_-]?token.*"));

    // Value patterns
    private static final Pattern JWT = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");
    private static final Pattern PEM_BLOCK = Pattern.compile("-----BEGIN ([A-Z ]+)-----");
    private static final Pattern BASIC_AUTH_IN_URL = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://[^/\\s]+@.*$");
    private static final Pattern LONG_BASE64ISH = Pattern.compile("^[A-Za-z0-9+/=_-]{80,}$"); // heuristic

    // Arguments like --password=foo, --token foo, --secret=foo, -Dkey=value, etc.
    private static final Pattern CLI_LONG_OPT_EQ = Pattern.compile(
            "(?i)(--[a-z0-9_.-]*(pass(word)?|secret|token|api[_-]?key|access[_-]?key|private[_-]?key|credential)[a-z0-9_.-]*=)([^\\s]+)");
    private static final Pattern CLI_LONG_OPT_SP = Pattern.compile(
            "(?i)(--[a-z0-9_.-]*(pass(word)?|secret|token|api[_-]?key|access[_-]?key|private[_-]?key|credential)[a-z0-9_.-]*)(\\s+)([^\\s]+)");
    private static final Pattern JAVA_PROP_EQ = Pattern.compile(
            "(?i)(-D[a-z0-9_.-]*(pass(word)?|secret|token|api[_-]?key|access[_-]?key|private[_-]?key|credential)[a-z0-9_.-]*=)([^\\s]+)");

    // Also mask common env-style assignments: FOO_TOKEN=bar
    private static final Pattern ENV_ASSIGN_EQ = Pattern.compile(
            "(?i)(\\b[a-z0-9_.-]*(pass(word)?|secret|token|api[_-]?key|access[_-]?key|private[_-]?key|credential)[a-z0-9_.-]*=)([^\\s]+)");

    private final ObjectMapper mapper;
    private final Set<String> allowKeysExact; // optional allowlist
    private final Set<String> denyKeysExact; // optional denylist
    private final boolean removeSecrets; // remove field entirely vs replace with "[REDACTED]"
    private final boolean scrubUrlsWithUserInfo; // remove user:pass@ from URLs

    public JsonSecretSanitizer(
            ObjectMapper mapper,
            Set<String> allowKeysExact,
            Set<String> denyKeysExact,
            boolean removeSecrets,
            boolean scrubUrlsWithUserInfo) {
        this.mapper = mapper;
        this.allowKeysExact = allowKeysExact == null ? Set.of() : canonicalSet(allowKeysExact);
        this.denyKeysExact = denyKeysExact == null ? Set.of() : canonicalSet(denyKeysExact);
        this.removeSecrets = removeSecrets;
        this.scrubUrlsWithUserInfo = scrubUrlsWithUserInfo;
    }

    public JsonNode sanitize(JsonNode root) {
        return sanitizeNode(root, null);
    }

    private JsonNode sanitizeNode(JsonNode node, String parentKey) {
        if (node == null || node.isNull())
            return node;

        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node.deepCopy();

            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            List<String> toRemove = new ArrayList<>();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                String key = e.getKey();
                JsonNode value = e.getValue();

                if (shouldRedactKey(key)) {
                    if (removeSecrets) {
                        toRemove.add(key);
                    } else {
                        obj.set(key, TextNode.valueOf("[REDACTED]"));
                    }
                    continue;
                }

                JsonNode sanitized = sanitizeNode(value, key);
                obj.set(key, sanitized);
            }

            for (String k : toRemove)
                obj.remove(k);
            return obj;
        }

        if (node.isArray()) {
            ArrayNode arr = mapper.createArrayNode();
            for (JsonNode item : node) {
                arr.add(sanitizeNode(item, parentKey));
            }
            return arr;
        }

        if (node.isTextual()) {
            String s = node.asText();

            // If the parent key is allowlisted, NEVER redact by value heuristics.
            // This is required for things like hashes (baconConfigSha, bacon-config-hash) and version/scmRevision.
            if (isAllowedKey(parentKey)) {
                if (isCliLikeKey(parentKey)) {
                    return TextNode.valueOf(sanitizeCliString(s));
                }

                // still optionally scrub URLs with embedded userinfo even on allowlisted keys
                if (scrubUrlsWithUserInfo && looksLikeUrlWithUserInfo(s)) {
                    return TextNode.valueOf(scrubUserInfoFromUrl(s));
                }
                return node;
            }

            // If this is a CLI-like field, sanitize arguments first (more precise than heuristics).
            if (isCliLikeKey(parentKey)) {
                s = sanitizeCliString(s);
                return TextNode.valueOf(s);
            }
            // scrub URLs with userinfo
            if (scrubUrlsWithUserInfo && looksLikeUrlWithUserInfo(s)) {
                return TextNode.valueOf(scrubUserInfoFromUrl(s));
            }

            // redact token-like strings
            if (looksLikeSecretValue(s)) {
                return TextNode.valueOf("[REDACTED]");
            }

            return node;
        }

        return node;
    }

    private boolean shouldRedactKey(String key) {
        String ck = canonicalKey(key);
        if (!allowKeysExact.isEmpty() && allowKeysExact.contains(ck)) {
            return false;
        }
        if (!denyKeysExact.isEmpty() && denyKeysExact.contains(ck)) {
            return true;
        }

        for (Pattern p : SENSITIVE_KEY_PATTERNS) {
            if (p.matcher(key).matches())
                return true;
        }

        if (ck.contains("secret")
                || ck.contains("token")
                || ck.contains("password")
                || ck.contains("credential")
                || ck.contains("apikey")
                || ck.contains("privatekey")) {
            return true;
        }

        return false;
    }

    private boolean isAllowedKey(String key) {
        if (key == null)
            return false;
        String ck = canonicalKey(key);
        return !allowKeysExact.isEmpty() && allowKeysExact.contains(ck);
    }

    private boolean looksLikeSecretValue(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty())
            return false;

        if (PEM_BLOCK.matcher(trimmed).find())
            return true;
        if (JWT.matcher(trimmed).matches())
            return true;
        if (LONG_BASE64ISH.matcher(trimmed).matches()) {
            // If it's pure hex, it's often just a hash, not a secret
            if (trimmed.matches("(?i)^[0-9a-f]{64,}$")) {
                return false;
            }
            return true;
        }

        // common token prefixes
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("bearer ") || lower.startsWith("basic "))
            return true;

        return false;
    }

    private boolean looksLikeUrlWithUserInfo(String s) {
        if (!BASIC_AUTH_IN_URL.matcher(s).matches())
            return false;
        try {
            URI uri = URI.create(s);
            return uri.getUserInfo() != null && !uri.getUserInfo().isBlank();
        } catch (Exception ignored) {
            return true; // if it matches heuristic, treat as risky
        }
    }

    private String scrubUserInfoFromUrl(String s) {
        try {
            URI uri = URI.create(s);
            if (uri.getUserInfo() == null)
                return s;
            URI cleaned = new URI(
                    uri.getScheme(),
                    null, // remove userinfo
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());
            return cleaned.toString();
        } catch (Exception e) {
            // last resort: strip up to '@' after scheme
            int at = s.indexOf('@');
            int scheme = s.indexOf("://");
            if (scheme >= 0 && at > scheme + 3) {
                return s.substring(0, scheme + 3) + s.substring(at + 1);
            }
            return "[REDACTED_URL]";
        }
    }

    private static String canonicalKey(String key) {
        // Normalize to make "bacon-config-hash" == "baconConfigHash" == "BACON_CONFIG_HASH"
        return key == null ? ""
                : key
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", ""); // remove -, _, spaces, etc
    }

    private static Set<String> canonicalSet(Set<String> in) {
        Set<String> out = new HashSet<>();
        for (String s : in)
            out.add(canonicalKey(s));
        return out;
    }

    private boolean isCliLikeKey(String key) {
        if (key == null)
            return false;
        String ck = canonicalKey(key);
        // canonicalKey removes punctuation, so: commandLine -> "commandline"
        return ck.equals("commandline")
                || ck.equals("buildscript")
                || ck.equals("buildscriptargs")
                || ck.equals("script")
                || ck.endsWith("commandline")
                || ck.endsWith("buildscript");
    }

    private String sanitizeCliString(String s) {
        if (s == null || s.isBlank())
            return s;

        String out = s;

        // Scrub any embedded userinfo in URLs inside a larger string
        if (scrubUrlsWithUserInfo) {
            out = out.replaceAll(
                    "(?i)([a-z][a-z0-9+.-]*://)([^\\s/@]+)@",
                    "$1[REDACTED]@");

        }

        out = CLI_LONG_OPT_EQ.matcher(out).replaceAll("$1[REDACTED]");
        out = CLI_LONG_OPT_SP.matcher(out).replaceAll("$1$3[REDACTED]");
        out = JAVA_PROP_EQ.matcher(out).replaceAll("$1[REDACTED]");
        out = ENV_ASSIGN_EQ.matcher(out).replaceAll("$1[REDACTED]");

        out = out.replaceAll("(?i)\\b(bearer)\\s+[A-Za-z0-9._-]+", "$1 [REDACTED]");
        out = out.replaceAll("(?i)\\b(basic)\\s+[A-Za-z0-9+/=._-]+", "$1 [REDACTED]");

        return out;
    }

}
