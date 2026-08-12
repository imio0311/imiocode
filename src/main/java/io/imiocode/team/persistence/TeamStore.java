package io.imiocode.team.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.team.TeamException;
import io.imiocode.team.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/** 版本化 team.json 仓储；所有变更均为整体原子替换。 */
public final class TeamStore {
    private final TeamPaths paths; private final ObjectMapper mapper=new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    public TeamStore(TeamPaths paths){this.paths=Objects.requireNonNull(paths);paths.ensureRoot();}
    public synchronized TeamConfig create(TeamConfig team){validatePaths(team);return CrossProcessFileLock.withLock(paths.lockFile(team.name(),"team"),()->{Path file=file(team.name());if(Files.exists(file,LinkOption.NOFOLLOW_LINKS))throw new TeamException("团队已存在: "+team.name());paths.verifyExisting(file.getParent());AtomicJsonFile.write(file,encode(team));return team;});}
    public synchronized Optional<TeamConfig> find(String name){Path file=file(name);if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))return Optional.empty();if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new TeamException("团队配置不是普通文件");return Optional.of(read(file));}
    public synchronized TeamConfig require(String name){return find(name).orElseThrow(()->new TeamException("团队不存在: "+name));}
    public synchronized TeamConfig update(String name,UnaryOperator<TeamConfig> change){return CrossProcessFileLock.withLock(paths.lockFile(name,"team"),()->{TeamConfig before=require(name);TeamConfig after=Objects.requireNonNull(change.apply(before));if(!before.name().equals(after.name()))throw new TeamException("不能重命名团队");validatePaths(after);AtomicJsonFile.write(file(name),encode(after));return after;});}
    public synchronized List<TeamConfig> list(){try(Stream<Path> entries=Files.list(paths.teamsRoot())){return entries.filter(p->Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS)).map(p->find(p.getFileName().toString()).orElse(null)).filter(Objects::nonNull).sorted(Comparator.comparing(TeamConfig::name)).toList();}catch(IOException e){throw new TeamException("无法列出团队",e);}}
    public synchronized void deleteMetadata(String name){Path lock=paths.lockFile(name,"team");CrossProcessFileLock.withLock(lock,()->{Path dir=paths.teamDirectory(name);try{if(Files.exists(dir))deleteTree(dir);return null;}catch(IOException e){throw new TeamException("无法删除团队元数据",e);}});try{Files.deleteIfExists(lock);Files.deleteIfExists(paths.lockFile(name,"tasks"));Files.deleteIfExists(paths.lockFile(name,"mailbox"));}catch(IOException ignored){}}
    private void deleteTree(Path root)throws IOException{try(Stream<Path>s=Files.walk(root)){for(Path p:s.sorted(Comparator.reverseOrder()).toList()){Path n=p.toAbsolutePath().normalize();if(!n.startsWith(paths.teamsRoot()))throw new TeamException("拒绝删除团队目录之外的路径");Files.deleteIfExists(n);}}}
    private Path file(String name){return paths.teamDirectory(name).resolve("team.json");}
    private String encode(TeamConfig team){ObjectNode root=mapper.createObjectNode();root.put("schemaVersion",team.schemaVersion());root.put("name",team.name());root.put("description",team.description());root.put("leadAgentId",team.leadAgentId());root.put("preferredBackend",team.preferredBackend().configValue());root.put("createdAt",team.createdAt().toString());root.put("updatedAt",team.updatedAt().toString());ObjectNode members=root.putObject("members");team.members().forEach((id,m)->{ObjectNode n=members.putObject(id);n.put("agentId",m.agentId());n.put("name",m.name());n.put("agentType",m.agentType());n.put("model",m.model());n.put("backend",m.backend().configValue());n.put("backendHandle",m.backendHandle());n.put("worktree",m.worktree().toString());n.put("status",m.status().name());if(m.planApprovalRequired()==null)n.putNull("planApprovalRequired");else n.put("planApprovalRequired",m.planApprovalRequired());n.put("lastActiveAt",m.lastActiveAt().toString());});try{return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);}catch(IOException e){throw new TeamException("无法编码团队配置",e);}}
    private TeamConfig read(Path file){try{JsonNode root=mapper.readTree(Files.readString(file,StandardCharsets.UTF_8));if(root==null||root.path("schemaVersion").asInt(-1)!=TeamConfig.SCHEMA_VERSION)throw new TeamException("团队配置版本无效");String name=text(root,"name");if(!file.getParent().getFileName().toString().equals(name))throw new TeamException("团队配置路径与名称不一致");Map<String,TeammateInfo> members=new LinkedHashMap<>();JsonNode items=root.path("members");if(!items.isObject())throw new TeamException("团队花名册无效");items.fields().forEachRemaining(e->{JsonNode n=e.getValue();Boolean approval=n.path("planApprovalRequired").isNull()||n.path("planApprovalRequired").isMissingNode()?null:n.path("planApprovalRequired").asBoolean();TeammateInfo m=new TeammateInfo(text(n,"agentId"),text(n,"name"),text(n,"agentType"),n.path("model").asText(""),TeamBackend.parse(text(n,"backend")),n.path("backendHandle").asText(""),Path.of(text(n,"worktree")),TeammateStatus.valueOf(text(n,"status")),approval,Instant.parse(text(n,"lastActiveAt")));if(members.putIfAbsent(e.getKey(),m)!=null)throw new TeamException("重复成员");});TeamConfig team=new TeamConfig(TeamConfig.SCHEMA_VERSION,name,root.path("description").asText(""),text(root,"leadAgentId"),TeamBackend.parse(text(root,"preferredBackend")),members,Instant.parse(text(root,"createdAt")),Instant.parse(text(root,"updatedAt")));validatePaths(team);return team;}catch(TeamException e){throw e;}catch(RuntimeException|IOException e){throw new TeamException("团队配置损坏",e);}}
    private void validatePaths(TeamConfig team){paths.requireSlug(team.name(),"team_name");paths.requireSlug(team.leadAgentId(),"lead_agent_id");for(var entry:team.members().entrySet()){paths.requireSlug(entry.getKey(),"agent_id");TeammateInfo member=entry.getValue();paths.requireSlug(member.agentId(),"agent_id");if(!entry.getKey().equals(member.agentId()))throw new TeamException("成员键与 agent ID 不一致");paths.verifyWorktree(member.worktree(),member.agentId().equals(team.leadAgentId()));}}
    private static String text(JsonNode node,String field){JsonNode value=node.get(field);if(value==null||!value.isTextual()||value.textValue().isBlank())throw new TeamException("团队配置缺少字段: "+field);return value.textValue();}
}
