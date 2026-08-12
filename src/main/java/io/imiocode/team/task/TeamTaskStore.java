package io.imiocode.team.task;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.imiocode.team.TeamException;
import io.imiocode.team.persistence.TeamPaths;
import io.imiocode.team.persistence.CrossProcessFileLock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** 持久任务图；一次锁内维护双向依赖并拒绝环。 */
public final class TeamTaskStore {
    private final TeamPaths paths;private final int maxTasks;private final Clock clock;private final ObjectMapper mapper=new ObjectMapper();
    public TeamTaskStore(TeamPaths paths,int maxTasks){this(paths,maxTasks,Clock.systemUTC());}
    TeamTaskStore(TeamPaths paths,int maxTasks,Clock clock){this.paths=paths;this.maxTasks=maxTasks;this.clock=clock;}
    public synchronized TeamTask create(String team,String title,String description,String assignee){return locked(team,()->{Map<String,TeamTask> tasks=read(team);if(tasks.size()>=maxTasks)throw new TeamException("团队任务数达到上限");String id=UUID.randomUUID().toString().substring(0,8);Instant now=clock.instant();TeamTask task=new TeamTask(id,team,title,description,TeamTaskStatus.PENDING,assignee,Set.of(),Set.of(),1,now,now,"");tasks.put(id,task);write(team,tasks);return task;});}
    public synchronized Optional<TeamTask> find(String team,String id){return Optional.ofNullable(read(team).get(id));}
    public synchronized TeamTask require(String team,String id){return find(team,id).orElseThrow(()->new TeamException("未知团队任务: "+id));}
    public synchronized List<TeamTask> list(String team){return read(team).values().stream().sorted(Comparator.comparing(TeamTask::createdAt)).toList();}
    public synchronized TeamTask update(String team,String id,TeamTaskStatus status,String assignee,String result,Long expectedVersion){return update(team,id,status,assignee,result,null,null,expectedVersion);}
    /** 一个 TaskUpdate 调用在同一文件锁和原子替换中完成字段及两类依赖更新。 */
    public synchronized TeamTask update(String team,String id,TeamTaskStatus status,String assignee,String result,String addBlocksOn,String addBlockedBy,Long expectedVersion){return locked(team,()->{Map<String,TeamTask> tasks=read(team);TeamTask old=require(tasks,id);checkVersion(old,expectedVersion);TeamTask next=old.changed(status==null?old.status():status,assignee==null?old.assigneeAgentId():assignee,old.blocksOn(),old.blockedBy(),result==null?old.result():result,clock.instant());tasks.put(id,next);if(addBlocksOn!=null&&!addBlocksOn.isBlank())link(tasks,id,addBlocksOn,true,clock.instant());if(addBlockedBy!=null&&!addBlockedBy.isBlank())link(tasks,id,addBlockedBy,false,clock.instant());validateGraph(team,tasks);write(team,tasks);return tasks.get(id);});}
    public synchronized TeamTask addBlocksOn(String team,String id,String targetId,Long expectedVersion){return addDependency(team,id,targetId,true,expectedVersion);}
    public synchronized TeamTask addBlockedBy(String team,String id,String blockerId,Long expectedVersion){return addDependency(team,id,blockerId,false,expectedVersion);}
    private TeamTask addDependency(String team,String id,String otherId,boolean outgoing,Long expectedVersion){if(id.equals(otherId))throw new TeamException("任务不能依赖自身");return locked(team,()->{Map<String,TeamTask> tasks=read(team);TeamTask current=require(tasks,id);checkVersion(current,expectedVersion);link(tasks,id,otherId,outgoing,clock.instant());validateGraph(team,tasks);write(team,tasks);return tasks.get(id);});}
    private static void link(Map<String,TeamTask> tasks,String id,String otherId,boolean outgoing,Instant now){if(id.equals(otherId))throw new TeamException("任务不能依赖自身");TeamTask current=require(tasks,id),other=require(tasks,otherId);if(outgoing){tasks.put(id,current.dependencies(add(current.blocksOn(),otherId),current.blockedBy(),now));tasks.put(otherId,other.dependencies(other.blocksOn(),add(other.blockedBy(),id),now));}else{tasks.put(id,current.dependencies(current.blocksOn(),add(current.blockedBy(),otherId),now));tasks.put(otherId,other.dependencies(add(other.blocksOn(),id),other.blockedBy(),now));}}
    private static Set<String> add(Set<String>s,String value){var n=new LinkedHashSet<>(s);n.add(value);return n;}
    private static boolean hasCycle(Map<String,TeamTask> tasks){Set<String> visiting=new HashSet<>(),done=new HashSet<>();for(String id:tasks.keySet())if(cycle(id,tasks,visiting,done))return true;return false;}
    private static boolean cycle(String id,Map<String,TeamTask> tasks,Set<String> visiting,Set<String> done){if(done.contains(id))return false;if(!visiting.add(id))return true;for(String next:tasks.get(id).blocksOn())if(cycle(next,tasks,visiting,done))return true;visiting.remove(id);done.add(id);return false;}
    private static void checkVersion(TeamTask t,Long expected){if(expected!=null&&t.version()!=expected)throw new TeamException("任务版本冲突");}
    private static TeamTask require(Map<String,TeamTask> tasks,String id){TeamTask t=tasks.get(id);if(t==null)throw new TeamException("未知团队任务: "+id);return t;}
    private Path file(String team){return paths.teamDirectory(team).resolve("tasks.json");}
    private <T>T locked(String team,java.util.function.Supplier<T> action){return CrossProcessFileLock.withLock(paths.lockFile(team,"tasks"),action);}
    private Map<String,TeamTask> read(String team){Path file=file(team);if(!Files.exists(file))return new LinkedHashMap<>();try{JsonNode root=mapper.readTree(Files.readString(file,StandardCharsets.UTF_8));if(root.path("schemaVersion").asInt()!=1)throw new TeamException("任务图版本无效");if(!team.equals(root.path("teamName").asText()))throw new TeamException("任务图跨团队");Map<String,TeamTask> out=new LinkedHashMap<>();for(JsonNode n:root.path("tasks")){TeamTask t=new TeamTask(text(n,"id"),text(n,"teamName"),text(n,"title"),n.path("description").asText(""),TeamTaskStatus.valueOf(text(n,"status")),n.path("assigneeAgentId").asText(""),strings(n.path("blocksOn")),strings(n.path("blockedBy")),n.path("version").asLong(),Instant.parse(text(n,"createdAt")),Instant.parse(text(n,"updatedAt")),n.path("result").asText(""));if(out.putIfAbsent(t.id(),t)!=null)throw new TeamException("重复任务");}validateGraph(team,out);return out;}catch(TeamException e){throw e;}catch(RuntimeException|IOException e){throw new TeamException("任务图损坏",e);}}
    private static void validateGraph(String team,Map<String,TeamTask> tasks){for(TeamTask task:tasks.values()){if(!team.equals(task.teamName()))throw new TeamException("任务记录跨团队");for(String target:task.blocksOn()){TeamTask other=require(tasks,target);if(!other.blockedBy().contains(task.id()))throw new TeamException("任务依赖双向关系不一致");}for(String blocker:task.blockedBy()){TeamTask other=require(tasks,blocker);if(!other.blocksOn().contains(task.id()))throw new TeamException("任务依赖双向关系不一致");}}if(hasCycle(tasks))throw new TeamException("任务依赖形成循环");}
    private void write(String team,Map<String,TeamTask> tasks){ObjectNode root=mapper.createObjectNode();root.put("schemaVersion",1);root.put("teamName",team);ArrayNode a=root.putArray("tasks");for(TeamTask t:tasks.values()){ObjectNode n=a.addObject();n.put("id",t.id());n.put("teamName",t.teamName());n.put("title",t.title());n.put("description",t.description());n.put("status",t.status().name());n.put("assigneeAgentId",t.assigneeAgentId());ArrayNode bo=n.putArray("blocksOn");t.blocksOn().forEach(bo::add);ArrayNode bb=n.putArray("blockedBy");t.blockedBy().forEach(bb::add);n.put("version",t.version());n.put("createdAt",t.createdAt().toString());n.put("updatedAt",t.updatedAt().toString());n.put("result",t.result());}writeAtomic(file(team),root.toPrettyString());}
    private static void writeAtomic(Path target,String data){try{Files.createDirectories(target.getParent());Path tmp=Files.createTempFile(target.getParent(),".tasks-",".tmp");try{Files.writeString(tmp,data+"\n",StandardCharsets.UTF_8);try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}}catch(IOException e){throw new TeamException("无法保存任务图",e);}}
    private static Set<String> strings(JsonNode n){if(!n.isArray())throw new TeamException("任务依赖无效");Set<String>s=new LinkedHashSet<>();n.forEach(v->{if(!v.isTextual()||v.textValue().isBlank())throw new TeamException("任务依赖无效");s.add(v.textValue());});return s;}
    private static String text(JsonNode n,String f){String v=n.path(f).asText("");if(v.isBlank())throw new TeamException("任务缺少字段: "+f);return v;}
}
