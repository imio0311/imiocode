package io.imiocode.team.persistence;

import java.time.Instant;

public record TranscriptEntry(int schemaVersion,String id,Instant createdAt,TranscriptRole role,String content,String correlationId) {
    public static final int SCHEMA_VERSION=1;
    public TranscriptEntry {if(schemaVersion!=1)throw new IllegalArgumentException("Transcript 版本无效");if(id==null||id.isBlank())throw new IllegalArgumentException("id 不能为空");if(createdAt==null||role==null)throw new IllegalArgumentException("记录时间和角色不能为空");content=content==null?"":content;correlationId=correlationId==null?"":correlationId;}
}
