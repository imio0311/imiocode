package io.imiocode.team.runtime;
import java.util.List;
public record SendReceipt(List<String> messageIds,List<String> warnings,List<String> resumedAgentIds) {
    public SendReceipt {messageIds=List.copyOf(messageIds);warnings=List.copyOf(warnings);resumedAgentIds=List.copyOf(resumedAgentIds);}
}
