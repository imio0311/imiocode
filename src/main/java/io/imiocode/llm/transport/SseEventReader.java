package io.imiocode.llm.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 将 UTF-8 Server-Sent Events 字节流解析为离散事件。
 *
 * <p>实现只消费当前 Provider 所需的 {@code event} 和 {@code data} 字段；注释行及其他 SSE 字段
 * 不参与模型流式响应组装。</p>
 */
public final class SseEventReader {
    /**
     * 持续读取输入流，并在一个 SSE 事件完整结束后交给消费者。
     *
     * @param input Provider 返回的 SSE 响应体
     * @param consumer 完整事件消费者
     * @throws IOException 读取响应体失败时抛出
     */
    public void read(InputStream input, Consumer<SseEvent> consumer) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(consumer, "consumer");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String event = null;
            StringBuilder data = new StringBuilder();
            boolean hasFields = false;
            String line;
            while ((line = reader.readLine()) != null) {
                // SSE 使用空行作为事件边界，多行 data 必须在此处一次性提交。
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
                    // 以冒号开头的是 keep-alive 注释，不属于模型输出。
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
            // 某些服务端在关闭连接前不发送最后一个空行，仍需交付已完整读取的字段。
            if (hasFields) {
                consumer.accept(new SseEvent(event, data.toString()));
            }
        }
    }
}
