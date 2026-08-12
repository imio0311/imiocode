package io.imiocode.team.mailbox;

import java.time.Instant;

public record MailboxMessage(int schemaVersion,String id,String teamName,String senderAgentId,
        String recipientAgentId,MailboxMessageType type,String summary,String body,String taskId,
        Instant createdAt,Instant consumedAt) {
    public static final int SCHEMA_VERSION=1;
    public MailboxMessage {
        if(schemaVersion!=SCHEMA_VERSION)throw new IllegalArgumentException("Mailbox 版本无效");
        id=text(id,"id");teamName=text(teamName,"teamName");senderAgentId=text(senderAgentId,"senderAgentId");recipientAgentId=text(recipientAgentId,"recipientAgentId");
        if(type==null)throw new IllegalArgumentException("type 不能为空");summary=summary==null?"":summary;body=body==null?"":body;taskId=taskId==null?"":taskId;if(createdAt==null)throw new IllegalArgumentException("createdAt 不能为空");
    }
    public MailboxMessage consumed(Instant at){return new MailboxMessage(schemaVersion,id,teamName,senderAgentId,recipientAgentId,type,summary,body,taskId,createdAt,at);}
    private static String text(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" 不能为空");return value.trim();}
}
