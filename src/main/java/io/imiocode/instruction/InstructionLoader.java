package io.imiocode.instruction;

/** 按用户级到项目近端的优先级加载并展开跨会话指令。 */
public interface InstructionLoader {
    InstructionSnapshot load(InstructionLoadRequest request);
}
