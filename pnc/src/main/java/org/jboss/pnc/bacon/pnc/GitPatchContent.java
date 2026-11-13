/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pnc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.Deflater;

/**
 * Writes file contents in git's patch formats, so that a diff can carry files whose content GitLab's API leaves out:
 * a whole new text file as a regular hunk, and anything else (binary content, large modified files) as a
 * {@code GIT binary patch} with full ("literal") contents, which {@code git apply} recreates byte for byte.
 */
final class GitPatchContent {

    static final String ZERO_ID = "0000000000000000000000000000000000000000";
    private static final String BASE85 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            + "!#$%&()*+-;<=>?@^_`{|}~";
    private static final int BYTES_PER_LINE = 52;
    private static final int TEXT_PROBE_BYTES = 8000;

    private GitPatchContent() {
    }

    /** The git object id of a blob with this content (SHA-1 of {@code "blob <size>\0" + content}). */
    static String blobId(byte[] content) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(("blob " + content.length + "\0").getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(sha1.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Whether the content can be written as text hunks: valid UTF-8 without NUL bytes in its beginning (git's own
     * binary heuristic looks for NUL in the first 8000 bytes).
     */
    static boolean isText(byte[] content) {
        for (int i = 0; i < Math.min(content.length, TEXT_PROBE_BYTES); i++) {
            if (content[i] == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * The hunk adding a whole text file, with the {@code ---}/{@code +++} lines. Empty content gives an empty string
     * (an empty new file only needs its header).
     */
    static String newTextFileHunk(String path, byte[] content) {
        if (content.length == 0) {
            return "";
        }
        String text = new String(content, StandardCharsets.UTF_8);
        boolean endsWithNewline = text.endsWith("\n");
        String[] lines = (endsWithNewline ? text.substring(0, text.length() - 1) : text).split("\n", -1);

        StringBuilder out = new StringBuilder();
        out.append("--- /dev/null\n");
        out.append("+++ b/").append(path).append('\n');
        out.append("@@ -0,0 +1").append(lines.length == 1 ? "" : "," + lines.length).append(" @@\n");
        for (String line : lines) {
            out.append('+').append(line).append('\n');
        }
        if (!endsWithNewline) {
            out.append("\\ No newline at end of file\n");
        }
        return out.toString();
    }

    /**
     * The {@code index} line and the {@code GIT binary patch} with the full new content (forward) and the full old
     * content (reverse, for {@code git apply -R}). {@code null} means the file does not exist on that side.
     */
    static String binaryPatch(byte[] oldContent, byte[] newContent) {
        byte[] before = oldContent == null ? new byte[0] : oldContent;
        byte[] after = newContent == null ? new byte[0] : newContent;
        return "index " + (oldContent == null ? ZERO_ID : blobId(before)) + ".."
                + (newContent == null ? ZERO_ID : blobId(after)) + "\n" + "GIT binary patch\n" + literal(after)
                + "\n" + literal(before) + "\n";
    }

    /** A {@code literal} binary hunk: the zlib-deflated content, in git's base85 lines. */
    static String literal(byte[] content) {
        byte[] deflated = deflate(content);
        StringBuilder out = new StringBuilder("literal ").append(content.length).append('\n');
        for (int offset = 0; offset < deflated.length; offset += BYTES_PER_LINE) {
            int length = Math.min(BYTES_PER_LINE, deflated.length - offset);
            out.append(length <= 26 ? (char) ('A' + length - 1) : (char) ('a' + length - 27));
            for (int group = 0; group < length; group += 4) {
                long value = 0;
                for (int i = 0; i < 4; i++) {
                    int index = offset + group + i;
                    int b = index < offset + length ? deflated[index] & 0xff : 0;
                    value = (value << 8) | b;
                }
                char[] chars = new char[5];
                for (int i = 4; i >= 0; i--) {
                    chars[i] = BASE85.charAt((int) (value % 85));
                    value /= 85;
                }
                out.append(chars);
            }
            out.append('\n');
        }
        return out.toString();
    }

    private static byte[] deflate(byte[] content) {
        // the level git uses for binary patches (zlib header 78 01), so the output is the same as git's
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(content);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, content.length / 2));
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer));
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }
}
