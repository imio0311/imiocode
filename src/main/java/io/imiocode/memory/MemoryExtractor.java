package io.imiocode.memory;

import io.imiocode.conversation.ChatMessage;
import java.util.List;

/**
 * 从一次已完成对话中提取可长期保存的记忆候选。
 *
 * <p>输入由调用方预先裁剪和脱敏；实现只返回候选，不直接写入用户或项目记忆文件。</p>
 */
public interface MemoryExtractor {
    MemoryExtractionResult extract(ChatMessage userMessage, List<ChatMessage> completedTrajectory,
                                   List<MemoryDocument> currentMemory);
}
