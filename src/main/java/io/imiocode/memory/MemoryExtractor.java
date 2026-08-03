package io.imiocode.memory;

import io.imiocode.conversation.ChatMessage;
import java.util.List;

public interface MemoryExtractor {
    MemoryExtractionResult extract(ChatMessage userMessage, List<ChatMessage> completedTrajectory,
                                   List<MemoryDocument> currentMemory);
}
