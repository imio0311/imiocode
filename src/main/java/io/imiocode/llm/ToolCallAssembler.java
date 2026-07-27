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
    private final Map<Integer, ToolCall> completed = new TreeMap<>();

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
        if (completed.containsKey(position)) {
            throw new IllegalArgumentException("工具调用已经完成");
        }
        Fragments fragments = calls.computeIfAbsent(position, ignored -> new Fragments());
        fragments.append(fragments.id, idFragment);
        fragments.append(fragments.name, nameFragment);
        if (argumentsFragment != null) {
            fragments.argumentsSeen = true;
            fragments.arguments.append(argumentsFragment);
        }
    }

    public void start(int position, String id, String name) {
        if (position < 0 || id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("工具调用缺少有效位置、标识或名称");
        }
        if (calls.containsKey(position) || completed.containsKey(position)) {
            throw new IllegalArgumentException("工具调用位置重复开始");
        }
        append(position, id, name, null);
    }

    public void appendArguments(int position, String fragment) {
        if (!calls.containsKey(position)) {
            throw new IllegalArgumentException("工具参数碎片没有对应调用");
        }
        append(position, null, null, fragment);
    }

    public ToolCall complete(int position) throws LlmException {
        if (completed.containsKey(position)) {
            throw protocolError("工具调用重复完成");
        }
        Fragments fragments = calls.remove(position);
        if (fragments == null) {
            throw protocolError("工具调用完成事件没有对应开始事件");
        }
        ToolCall call = parse(fragments);
        completed.put(position, call);
        return call;
    }

    public boolean hasOpenCalls() {
        return !calls.isEmpty();
    }

    public void ensureEmptyArguments(int position) {
        Fragments fragments = calls.get(position);
        if (fragments != null && !fragments.argumentsSeen) {
            fragments.argumentsSeen = true;
            fragments.arguments.append("{}");
        }
    }

    public boolean isEmpty() {
        return calls.isEmpty() && completed.isEmpty();
    }

    public List<ToolCall> finish() throws LlmException {
        for (Integer position : new ArrayList<>(calls.keySet())) {
            complete(position);
        }
        return List.copyOf(completed.values());
    }

    private ToolCall parse(Fragments fragments) throws LlmException {
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
            return new ToolCall(id, name, object);
        } catch (IOException exception) {
            throw protocolError("工具调用参数不是有效 JSON");
        }
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
