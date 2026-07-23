package io.imiocode.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.ToolCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** 按调用位置隔离并拼接流式工具标识、名称和 JSON 参数碎片。 */
public final class ToolCallAssembler {
    private final ObjectMapper objectMapper;
    private final Map<Integer, Fragments> calls = new TreeMap<>();

    public ToolCallAssembler(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public void append(
            int position,
            String idFragment,
            String nameFragment,
            String argumentsFragment) {
        if (position < 0) {
            throw new IllegalArgumentException("工具调用位置不能为负数");
        }
        Fragments fragments = calls.computeIfAbsent(position, ignored -> new Fragments());
        fragments.append(fragments.id, idFragment);
        fragments.append(fragments.name, nameFragment);
        if (argumentsFragment != null) {
            fragments.argumentsSeen = true;
            fragments.arguments.append(argumentsFragment);
        }
    }

    public void ensureEmptyArguments(int position) {
        Fragments fragments = calls.get(position);
        if (fragments != null && !fragments.argumentsSeen) {
            fragments.argumentsSeen = true;
            fragments.arguments.append("{}");
        }
    }

    public boolean isEmpty() {
        return calls.isEmpty();
    }

    public List<ToolCall> finish() throws LlmException {
        List<ToolCall> result = new ArrayList<>();
        for (Fragments fragments : calls.values()) {
            String id = fragments.id.toString();
            String name = fragments.name.toString();
            String arguments = fragments.arguments.toString();
            if (id.isBlank() || name.isBlank() || !fragments.argumentsSeen || arguments.isBlank()) {
                throw protocolError("工具调用缺少标识、名称或参数");
            }
            try {
                JsonNode parsed = objectMapper.readTree(arguments);
                if (!(parsed instanceof ObjectNode object)) {
                    throw protocolError("工具调用参数必须是 JSON 对象");
                }
                result.add(new ToolCall(id, name, object));
            } catch (IOException exception) {
                throw protocolError("工具调用参数不是有效 JSON");
            }
        }
        return List.copyOf(result);
    }

    private static LlmException protocolError(String message) {
        return new LlmException(LlmErrorType.PROTOCOL, true, null, message);
    }

    private static final class Fragments {
        private final StringBuilder id = new StringBuilder();
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();
        private boolean argumentsSeen;

        private void append(StringBuilder target, String fragment) {
            if (fragment != null && !fragment.isEmpty()) {
                target.append(fragment);
            }
        }
    }
}
