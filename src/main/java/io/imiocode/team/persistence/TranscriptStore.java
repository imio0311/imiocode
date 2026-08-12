package io.imiocode.team.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.team.TeamException;
import io.imiocode.tool.SecretRedactor;
import java.time.*;
import java.util.*;

/** 成员独立 transcript，续写时加载全部完整记录。 */
public final class TranscriptStore {
    private final TeamPaths paths;private final JsonlLog log;private final SecretRedactor redactor;private final ObjectMapper mapper=new ObjectMapper();private final int maxEntryChars;private final Clock clock;
    public TranscriptStore(TeamPaths paths,SecretRedactor redactor,int maxEntryChars,int maxBytes){this(paths,redactor,maxEntryChars,maxBytes,Clock.systemUTC());}
    TranscriptStore(TeamPaths paths,SecretRedactor redactor,int maxEntryChars,int maxBytes,Clock clock){this.paths=paths;this.redactor=redactor;this.maxEntryChars=maxEntryChars;this.log=new JsonlLog(maxBytes);this.clock=clock;}
    public synchronized TranscriptEntry append(String team,String agent,TranscriptRole role,String content,String correlationId){String safe=redactor.redact(Objects.requireNonNullElse(content,""));if(safe.length()>maxEntryChars)throw new IllegalArgumentException("Transcript 单条记录超过大小上限");TranscriptEntry e=new TranscriptEntry(1,UUID.randomUUID().toString(),clock.instant(),role,safe,correlationId);ObjectNode n=mapper.createObjectNode();n.put("schemaVersion",1);n.put("id",e.id());n.put("createdAt",e.createdAt().toString());n.put("role",e.role().name());n.put("content",e.content());n.put("correlationId",e.correlationId());log.append(paths.transcriptFile(team,agent),n.toString());return e;}
    public synchronized List<TranscriptEntry> load(String team,String agent){List<TranscriptEntry> out=new ArrayList<>();for(String line:log.readRecoveringTail(paths.transcriptFile(team,agent))){try{JsonNode n=mapper.readTree(line);if(n.path("schemaVersion").asInt()!=1)throw new TeamException("Transcript 版本无效");out.add(new TranscriptEntry(1,text(n,"id"),Instant.parse(text(n,"createdAt")),TranscriptRole.valueOf(text(n,"role")),n.path("content").asText(""),n.path("correlationId").asText("")));}catch(RuntimeException|java.io.IOException e){throw new TeamException("Transcript 记录损坏",e);}}return List.copyOf(out);}
    private static String text(JsonNode n,String f){String v=n.path(f).asText("");if(v.isBlank())throw new TeamException("Transcript 缺少字段: "+f);return v;}
}
