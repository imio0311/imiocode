package io.imiocode.tool.workspace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** 严格解码并限制字节数、行数的 UTF-8 文本读取器。 */
public final class Utf8TextFile {
    private Utf8TextFile() {
    }

    public static ReadResult read(
            Path path,
            long maxBytes,
            int maxLines,
            BooleanSupplier cancelled) throws IOException, InterruptedException {
        if (maxBytes <= 0 || maxLines <= 0) {
            throw new IllegalArgumentException("读取限制必须为正数");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean byteTruncated = false;
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (cancelled.getAsBoolean()) {
                    throw new InterruptedException("读取已取消");
                }
                int remaining = (int) Math.min(Integer.MAX_VALUE, maxBytes - bytes.size());
                if (remaining <= 0) {
                    byteTruncated = true;
                    break;
                }
                int kept = Math.min(count, remaining);
                for (int index = 0; index < kept; index++) {
                    if (buffer[index] == 0) {
                        throw new IllegalArgumentException("文件包含 NUL 字节，可能是二进制文件");
                    }
                }
                bytes.write(buffer, 0, kept);
                if (kept < count) {
                    byteTruncated = true;
                    break;
                }
            }
        }

        String text = decode(bytes.toByteArray(), byteTruncated);
        List<String> allLines = splitLines(text);
        boolean lineTruncated = allLines.size() > maxLines;
        List<String> lines = lineTruncated
                ? List.copyOf(allLines.subList(0, maxLines))
                : List.copyOf(allLines);
        return new ReadResult(text, lines, byteTruncated || lineTruncated);
    }

    private static String decode(byte[] bytes, boolean truncated) {
        int attempts = truncated ? Math.min(3, bytes.length) : 0;
        for (int trim = 0; trim <= attempts; trim++) {
            try {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes, 0, bytes.length - trim))
                        .toString();
            } catch (CharacterCodingException exception) {
                // 字节上限可能落在多字节字符中，最多只丢弃一个 UTF-8 字符的残片。
            }
        }
        throw new IllegalArgumentException("文件不是有效的 UTF-8 文本");
    }

    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            return lines;
        }
        int start = 0;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '\n' || current == '\r') {
                lines.add(text.substring(start, index));
                if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                start = index + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        return lines;
    }

    public record ReadResult(String text, List<String> lines, boolean truncated) {
    }
}
