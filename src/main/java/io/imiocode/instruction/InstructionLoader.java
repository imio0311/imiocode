package io.imiocode.instruction;

public interface InstructionLoader {
    InstructionSnapshot load(InstructionLoadRequest request);
}
