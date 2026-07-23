package io.imiocode.llm.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

public final class SseEventReader {
    public void read(InputStream input, Consumer<SseEvent> consumer) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(consumer, "consumer");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String event = null;
            StringBuilder data = new StringBuilder();
            boolean hasFields = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (hasFields) {
                        consumer.accept(new SseEvent(event, data.toString()));
                    }
                    event = null;
                    data.setLength(0);
                    hasFields = false;
                    continue;
                }
                if (line.startsWith(":")) {
                    continue;
                }

                int colon = line.indexOf(':');
                String field = colon < 0 ? line : line.substring(0, colon);
                String value = colon < 0 ? "" : line.substring(colon + 1);
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                if ("event".equals(field)) {
                    event = value;
                    hasFields = true;
                } else if ("data".equals(field)) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(value);
                    hasFields = true;
                }
            }
            if (hasFields) {
                consumer.accept(new SseEvent(event, data.toString()));
            }
        }
    }
}
