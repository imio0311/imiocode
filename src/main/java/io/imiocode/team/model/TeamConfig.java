package io.imiocode.team.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record TeamConfig(int schemaVersion,String name,String description,String leadAgentId,
        TeamBackend preferredBackend,Map<String,TeammateInfo> members,Instant createdAt,Instant updatedAt) {
    public static final int SCHEMA_VERSION=1;
    public TeamConfig {
        if(schemaVersion!=SCHEMA_VERSION)throw new IllegalArgumentException("团队配置版本无效");
        name=text(name,"name");description=description==null?"":description.trim();leadAgentId=text(leadAgentId,"leadAgentId");
        Objects.requireNonNull(preferredBackend);members=Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(members)));
        if(!members.containsKey(leadAgentId))throw new IllegalArgumentException("团队花名册缺少 Lead");
        for (Map.Entry<String, TeammateInfo> entry : members.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().agentId())) {
                throw new IllegalArgumentException("成员键与 agent ID 不一致");
            }
        }
        Objects.requireNonNull(createdAt);Objects.requireNonNull(updatedAt);
    }
    public TeamConfig withMember(TeammateInfo member,Instant now){var next=new LinkedHashMap<>(members);if(next.putIfAbsent(member.agentId(),member)!=null)throw new IllegalArgumentException("成员已存在: "+member.agentId());return new TeamConfig(schemaVersion,name,description,leadAgentId,preferredBackend,next,createdAt,now);}
    public TeamConfig replaceMember(TeammateInfo member,Instant now){if(!members.containsKey(member.agentId()))throw new IllegalArgumentException("未知成员: "+member.agentId());var next=new LinkedHashMap<>(members);next.put(member.agentId(),member);return new TeamConfig(schemaVersion,name,description,leadAgentId,preferredBackend,next,createdAt,now);}
    public TeamConfig withoutMember(String agentId,Instant now){if(leadAgentId.equals(agentId))throw new IllegalArgumentException("不能移除 Lead");var next=new LinkedHashMap<>(members);next.remove(agentId);return new TeamConfig(schemaVersion,name,description,leadAgentId,preferredBackend,next,createdAt,now);}
    private static String text(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" 不能为空");return value.trim();}
}
