package io.imiocode.team.runtime;

import io.imiocode.team.TeamException;
import io.imiocode.team.backend.*;
import io.imiocode.team.config.TeamRuntimeConfig;
import io.imiocode.team.mailbox.*;
import io.imiocode.team.model.*;
import io.imiocode.team.persistence.*;
import io.imiocode.team.task.*;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import io.imiocode.worktree.model.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 团队生命周期唯一编排入口。 */
public final class AgentTeamManager implements AutoCloseable, TeamMessenger {
    private final Path repositoryRoot;private final TeamRuntimeConfig config;private final TeamPaths paths;private final TeamStore teams;private final MailboxStore mailbox;private final TranscriptStore transcripts;private final TeamTaskStore tasks;private final BackendSelector backends;private final WorktreeManager worktrees;private final TeamMemberRunner runner;private final Clock clock;private final TeamMemberWorker worker;
    private final Map<String,BackendHandle> handles=new ConcurrentHashMap<>();private final Map<String,WorktreeLease> leases=new ConcurrentHashMap<>();
    public AgentTeamManager(Path repositoryRoot,TeamRuntimeConfig config,TeamPaths paths,TeamStore teams,MailboxStore mailbox,TranscriptStore transcripts,TeamTaskStore tasks,BackendSelector backends,WorktreeManager worktrees,TeamMemberRunner runner){this(repositoryRoot,config,paths,teams,mailbox,transcripts,tasks,backends,worktrees,runner,Clock.systemUTC());}
    AgentTeamManager(Path repositoryRoot,TeamRuntimeConfig config,TeamPaths paths,TeamStore teams,MailboxStore mailbox,TranscriptStore transcripts,TeamTaskStore tasks,BackendSelector backends,WorktreeManager worktrees,TeamMemberRunner runner,Clock clock){this.repositoryRoot=repositoryRoot.toAbsolutePath().normalize();this.config=config;this.paths=paths;this.teams=teams;this.mailbox=mailbox;this.transcripts=transcripts;this.tasks=tasks;this.backends=backends;this.worktrees=worktrees;this.runner=runner;this.clock=clock;this.worker=new TeamMemberWorker(teams,mailbox,transcripts,runner,clock);}
    public synchronized TeamConfig createTeam(TeamCreateRequest r){if(teams.list().size()>=config.maxTeams())throw new TeamException("团队数量达到上限");String team=paths.requireSlug(r.name(),"team_name"),leadId=paths.requireSlug(blank(r.leadAgentId())?"lead":r.leadAgentId(),"lead_agent_id");Instant now=clock.instant();TeammateInfo lead=new TeammateInfo(leadId,blank(r.leadName())?"Lead":r.leadName(),blank(r.defaultAgentType())?"lead":r.defaultAgentType(),"",TeamBackend.IN_PROCESS,"lead",repositoryRoot,TeammateStatus.RUNNING,false,now);return teams.create(new TeamConfig(1,team,Objects.requireNonNullElse(r.description(),""),leadId,r.backend()==null?config.preferredBackend():r.backend(),Map.of(leadId,lead),now,now));}
    /** 重启时外部运行句柄不可盲目信任；保留身份和上下文，把活动队员安全降为 stopped。 */
    public synchronized List<TeamConfig> recover(){List<TeamConfig> recovered=new ArrayList<>();for(TeamConfig team:teams.list()){TeamConfig current=team;for(TeammateInfo member:new ArrayList<>(current.members().values())){if(!member.agentId().equals(current.leadAgentId())&&member.status().active()){TeammateInfo stopped=member.withStatus(TeammateStatus.STOPPED,clock.instant());current=teams.update(current.name(),t->t.replaceMember(stopped,clock.instant()));}}recovered.add(current);}return List.copyOf(recovered);}
    public synchronized TeammateInfo spawn(TeamPrincipal lead,TeammateSpawnRequest r){
        TeamConfig team=requireLead(lead);
        if(team.members().size()>=config.maxMembersPerTeam())throw new TeamException("团队成员数达到上限");
        String type=blank(r.agentType())?"general-purpose":r.agentType().trim();
        String name=blank(r.name())?nextName(team,type):paths.requireSlug(r.name(),"member_name");
        if(team.members().values().stream().anyMatch(m->m.name().equals(name)||m.agentId().equals(name)))throw new TeamException("成员名称已存在: "+name);
        WorktreeLease lease=null;BackendHandle handle=null;boolean registered=false;
        var gate=new java.util.concurrent.CountDownLatch(1);
        var cancelled=new java.util.concurrent.atomic.AtomicBoolean();
        try{
            if(worktrees==null)throw new TeamException("团队成员需要 Git Worktree");
            lease=worktrees.createAgentWorktree(team.name()+"-"+name);
            String id=name;Instant now=clock.instant();
            TeammateInfo starting=new TeammateInfo(id,name,type,Objects.requireNonNullElse(r.model(),""),TeamBackend.AUTO,"pending",lease.workdir(),TeammateStatus.STARTING,r.planApprovalRequired(),now);
            teams.update(team.name(),t->t.withMember(starting,now));registered=true;
            TeamBackend requested=r.backend()==null||r.backend()==TeamBackend.AUTO?team.preferredBackend():r.backend();
            List<String>warnings=new ArrayList<>();WorktreeLease activeLease=lease;
            Runnable task=()->{try{gate.await();if(!cancelled.get())worker.run(team.name(),id,type,activeLease.workdir());}catch(InterruptedException e){Thread.currentThread().interrupt();}};
            TeammateLaunchRequest launch=new TeammateLaunchRequest(team.name(),id,repositoryRoot,lease.workdir(),memberCommand(team.name(),id,lease.workdir()),task);
            handle=backends.start(requested,launch,warnings);
            handles.put(key(team.name(),id),handle);leases.put(key(team.name(),id),lease);
            TeammateInfo running=starting.withBackend(handle.backend(),handle.value(),TeammateStatus.RUNNING,clock.instant());
            teams.update(team.name(),t->t.replaceMember(running,clock.instant()));
            mailbox.send(team.name(),lead.agentId(),id,MailboxMessageType.TASK,"初始任务",Objects.requireNonNullElse(r.prompt(),""),"");
            gate.countDown();return running;
        }catch(RuntimeException e){
            cancelled.set(true);gate.countDown();
            if(handle!=null)try{backends.require(handle.backend()).stop(handle,config.shutdownTimeout());}catch(RuntimeException ignored){}
            handles.remove(key(team.name(),name));leases.remove(key(team.name(),name));
            if(registered)try{teams.update(team.name(),t->t.withoutMember(name,clock.instant()));}catch(RuntimeException ignored){}
            if(lease!=null)lease.closeSafely();throw e;
        }
    }
    public synchronized SendReceipt send(TeamPrincipal sender,String recipient,MailboxMessageType type,String summary,String body,String taskId){
        TeamConfig team=requireMember(sender);
        if(type==MailboxMessageType.PLAN_APPROVAL){
            if(!sender.lead())throw new TeamException("只有 Lead 可以审批计划");
            String decision=Objects.requireNonNullElse(summary,"").trim().toUpperCase(Locale.ROOT);
            if(!decision.equals("APPROVED")&&!decision.equals("REJECTED"))throw new TeamException("计划审批摘要必须是 APPROVED 或 REJECTED");
            if(decision.equals("REJECTED")&&blank(body))throw new TeamException("拒绝计划时必须提供反馈");
            summary=decision;
        }
        List<MailboxMessage> sent="*".equals(recipient)
                ?mailbox.broadcast(team.name(),sender.agentId(),team.members().keySet(),type,summary,body,taskId)
                :List.of(sendOne(team,sender,recipient,type,summary,body,taskId));
        List<String>warnings=new ArrayList<>(),resumed=new ArrayList<>();
        for(MailboxMessage message:sent){
            TeammateInfo target=team.members().get(message.recipientAgentId());String memberKey=key(team.name(),target.agentId());
            BackendHandle handle=handles.get(memberKey);boolean wakeFailed=false;
            boolean stoppedStatus=target.status()==TeammateStatus.STOPPED||target.status()==TeammateStatus.FAILED;
            if(handle!=null&&stoppedStatus){
                try{backends.require(handle.backend()).stop(handle,config.shutdownTimeout());handles.remove(memberKey,handle);handle=null;}
                catch(RuntimeException stopFailure){warnings.add("成员 "+target.name()+" 旧运行句柄无法安全停止，未重复启动");}
            }else if(handle!=null){
                try{backends.require(handle.backend()).wake(handle);}
                catch(RuntimeException wakeFailure){
                    wakeFailed=true;warnings.add("成员 "+target.name()+" 唤醒失败，消息已持久化");
                    try{backends.require(handle.backend()).stop(handle,config.shutdownTimeout());handles.remove(memberKey,handle);handle=null;}
                    catch(RuntimeException stopFailure){warnings.add("成员 "+target.name()+" 旧运行句柄无法安全停止，未重复启动");}
                }
            }
            if(target.status()==TeammateStatus.IDLE){
                if(handle==null){resume(team,target,warnings);resumed.add(target.agentId());}
                else if(wakeFailed)warnings.add("成员 "+target.name()+" 将继续通过轮询读取消息");
                else resumed.add(target.agentId());
            }else if(stoppedStatus&&handle==null){
                resume(team,target,warnings);resumed.add(target.agentId());
            }
        }
        return new SendReceipt(sent.stream().map(MailboxMessage::id).toList(),warnings,resumed);
    }
    private MailboxMessage sendOne(TeamConfig team,TeamPrincipal sender,String recipient,MailboxMessageType type,String summary,String body,String taskId){if(!team.members().containsKey(recipient))throw new TeamException("收件人不在团队中");return mailbox.send(team.name(),sender.agentId(),recipient,type,summary,body,taskId);}
    private void resume(TeamConfig team,TeammateInfo target,List<String>warnings){var gate=new java.util.concurrent.CountDownLatch(1);var cancelled=new java.util.concurrent.atomic.AtomicBoolean();Runnable task=()->{try{gate.await();if(!cancelled.get())worker.run(team.name(),target.agentId(),target.agentType(),target.worktree());}catch(InterruptedException e){Thread.currentThread().interrupt();}};TeammateLaunchRequest launch=new TeammateLaunchRequest(team.name(),target.agentId(),repositoryRoot,target.worktree(),memberCommand(team.name(),target.agentId(),target.worktree()),task);try{BackendHandle h=backends.start(target.backend(),launch,warnings);handles.put(key(team.name(),target.agentId()),h);markStatus(team.name(),target.agentId(),TeammateStatus.RUNNING);gate.countDown();}catch(RuntimeException e){cancelled.set(true);gate.countDown();warnings.add("成员 "+target.name()+" 恢复失败");}}
    public synchronized boolean stop(String team,String agent){TeamConfig cfg=teams.require(team);TeammateInfo member=cfg.members().get(agent);if(member==null)return false;String memberKey=key(team,agent);BackendHandle h=handles.get(memberKey);if(h!=null){backends.require(h.backend()).stop(h,config.shutdownTimeout());handles.remove(memberKey,h);}markStatus(team,agent,TeammateStatus.STOPPED);return true;}
    public synchronized ConvergenceReport converge(TeamPrincipal lead){return converge(lead,Duration.ZERO);}
    public synchronized ConvergenceReport converge(TeamPrincipal lead,Duration timeout){
        TeamConfig team=requireLead(lead);Duration wait=Objects.requireNonNullElse(timeout,Duration.ZERO);
        if(wait.isNegative()||wait.compareTo(Duration.ofSeconds(30))>0)throw new TeamException("收敛等待必须在 0..30 秒之间");
        long deadline=System.nanoTime()+wait.toNanos();ConvergenceReport report;
        do{report=convergenceSnapshot(team.name());if(report.converged()||System.nanoTime()>=deadline)return report;
            try{Thread.sleep(50);}catch(InterruptedException e){Thread.currentThread().interrupt();return report;}
        }while(true);
    }
    private ConvergenceReport convergenceSnapshot(String teamName){
        TeamConfig team=teams.require(teamName);
        List<String> members=team.members().values().stream().map(m->m.agentId()+"="+m.status()).toList();
        List<String> results=new ArrayList<>();
        for(TeammateInfo member:team.members().values()){
            if(member.agentId().equals(team.leadAgentId()))continue;
            try{transcripts.load(team.name(),member.agentId()).stream()
                    .filter(entry->entry.role()==TranscriptRole.ASSISTANT).reduce((a,b)->b)
                    .ifPresent(entry->results.add(member.agentId()+"="+abbreviate(entry.content(),500)));
            }catch(RuntimeException e){results.add(member.agentId()+"=<transcript unavailable>");}
        }
        List<TeamTask> teamTasks=tasks.list(team.name());
        List<String> taskStates=teamTasks.stream().map(t->t.id()+"="+t.status()).toList();
        List<String> unread=new ArrayList<>();for(String id:team.members().keySet()){int count=mailbox.unread(team.name(),id).size();if(count>0)unread.add(id+"="+count);}
        List<String> wt=new ArrayList<>();if(worktrees!=null){Set<Path> owned=ownedWorktreePaths(team);for(ManagedWorktree w:worktrees.list())if(owned.contains(normalize(w.path())))wt.add(w.branch()+" dirty="+w.changes().changedFiles()+" commits="+w.changes().uniqueCommits());}
        boolean done=teamTasks.stream().allMatch(t->t.status().terminal())&&team.members().values().stream().filter(m->!m.agentId().equals(team.leadAgentId())).noneMatch(m->m.status().active());
        return new ConvergenceReport(team.name(),done,members,results,taskStates,unread,wt);
    }
    public synchronized TeamDeletionReport deleteTeam(TeamPrincipal lead,boolean discard){TeamConfig team=requireLead(lead);List<String>retained=new ArrayList<>(),warnings=new ArrayList<>(),blockers=new ArrayList<>();if(!discard){List<String>pending=unreadImportant(team);if(!pending.isEmpty())return new TeamDeletionReport(false,pending,List.of("存在未消费的重要 Mailbox 消息，默认拒绝删除"));}for(TeammateInfo member:team.members().values()){if(member.agentId().equals(team.leadAgentId()))continue;if(member.status()!=TeammateStatus.STOPPED&&member.status()!=TeammateStatus.FAILED)requestStop(team,member,warnings);}boolean stopped=waitForStop(team);if(!stopped&&!discard)return new TeamDeletionReport(false,List.of(),List.of("成员未在关闭超时内停止，团队已保留"));for(TeammateInfo member:team.members().values())if(!member.agentId().equals(team.leadAgentId()))try{stop(team.name(),member.agentId());}catch(RuntimeException e){blockers.add("成员 "+member.name()+" 停止失败，运行句柄已保留");}if(!blockers.isEmpty())return new TeamDeletionReport(false,retained,merge(warnings,blockers));for(var e:new ArrayList<>(leases.entrySet()))if(e.getKey().startsWith(team.name()+"/")){WorktreeCleanupReport report=e.getValue().closeSafely();leases.remove(e.getKey());if(report!=null&&!report.removed())retained.add(report.path().toString());}if(!retained.isEmpty()&&!discard)return new TeamDeletionReport(false,retained,merge(warnings,List.of("存在有改动或提交的 Worktree，已安全保留")));if(discard&&worktrees!=null){Set<Path>owned=ownedWorktreePaths(team);for(ManagedWorktree w:new ArrayList<>(worktrees.list()))if(owned.contains(normalize(w.path())))try{worktrees.remove(w.slug(),true);}catch(RuntimeException e){blockers.add("无法清理 "+w.path());}}if(!blockers.isEmpty())return new TeamDeletionReport(false,retained,merge(warnings,blockers));teams.deleteMetadata(team.name());return new TeamDeletionReport(true,retained,warnings);}
    private List<String> unreadImportant(TeamConfig team){List<String>pending=new ArrayList<>();for(String agent:team.members().keySet()){long count=mailbox.unread(team.name(),agent).stream().filter(message->message.type()!=MailboxMessageType.IDLE&&message.type()!=MailboxMessageType.STOP_RESPONSE).count();if(count>0)pending.add(agent+"="+count);}return pending;}
    private void requestStop(TeamConfig team,TeammateInfo member,List<String>warnings){try{mailbox.send(team.name(),team.leadAgentId(),member.agentId(),MailboxMessageType.STOP_REQUEST,"停止成员","","");BackendHandle handle=handles.get(key(team.name(),member.agentId()));if(handle!=null)backends.require(handle.backend()).wake(handle);}catch(RuntimeException e){warnings.add("成员 "+member.name()+" 停止请求投递或唤醒失败");}}
    private boolean waitForStop(TeamConfig team){long deadline=System.nanoTime()+config.shutdownTimeout().toNanos();while(System.nanoTime()<deadline){boolean allStopped=teams.require(team.name()).members().values().stream().filter(member->!member.agentId().equals(team.leadAgentId())).allMatch(member->member.status()==TeammateStatus.STOPPED||member.status()==TeammateStatus.FAILED);if(allStopped)return true;try{Thread.sleep(50);}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}}return false;}
    private TeamConfig requireLead(TeamPrincipal p){TeamConfig t=requireMember(p);if(!p.lead()||!t.leadAgentId().equals(p.agentId()))throw new TeamException("只有 Lead 可以执行此操作");return t;}
    private TeamConfig requireMember(TeamPrincipal p){TeamConfig t=teams.require(p.teamName());if(!t.members().containsKey(p.agentId()))throw new TeamException("调用者不在团队中");boolean actualLead=t.leadAgentId().equals(p.agentId());if(actualLead!=p.lead())throw new TeamException("团队角色与花名册身份不匹配");return t;}
    private void markStatus(String team,String agent,TeammateStatus status){teams.update(team,t->{TeammateInfo m=t.members().get(agent);return t.replaceMember(m.withStatus(status,clock.instant()),clock.instant());});}
    private List<String> memberCommand(String team,String agent,Path worktree){return List.of(javaCommand(),"-jar",jarPath(),"--team-member",team,agent,"--repository",repositoryRoot.toString(),"--workspace",worktree.toString());}
    private static String javaCommand(){return Path.of(System.getProperty("java.home"),"bin","java").toString();}
    private static String jarPath(){String configured=System.getProperty("imiocode.jar");if(configured!=null&&!configured.isBlank())return Path.of(configured).toAbsolutePath().normalize().toString();try{Path location=Path.of(io.imiocode.ImioCodeApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());if(location.toString().endsWith(".jar"))return location.toAbsolutePath().normalize().toString();}catch(Exception ignored){}return Path.of("target","imiocode-0.2.0-SNAPSHOT-all.jar").toAbsolutePath().normalize().toString();}
    private static String nextName(TeamConfig team,String type){String base=type.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");if(base.isBlank())base="agent";for(int i=1;i<=999;i++){String n=base+"-"+i;if(!team.members().containsKey(n))return n;}throw new TeamException("无法生成成员名称");}
    private static Set<Path> ownedWorktreePaths(TeamConfig team){Set<Path>paths=new HashSet<>();for(TeammateInfo member:team.members().values())if(!member.agentId().equals(team.leadAgentId()))paths.add(normalize(member.worktree()));return paths;}
    private static Path normalize(Path path){return path.toAbsolutePath().normalize();}
    private static String abbreviate(String value,int max){String text=Objects.requireNonNullElse(value,"");return text.length()<=max?text:text.substring(0,max-1)+"…";}
    private static List<String> merge(List<String> first,List<String> second){List<String>out=new ArrayList<>(first);out.addAll(second);return List.copyOf(out);}
    private static String key(String team,String agent){return team+"/"+agent;}
    private static boolean blank(String value){return value==null||value.isBlank();}
    @Override public synchronized void close(){for(String k:new ArrayList<>(handles.keySet())){int slash=k.indexOf('/');try{stop(k.substring(0,slash),k.substring(slash+1));}catch(RuntimeException ignored){}}for(WorktreeLease lease:leases.values())lease.release();leases.clear();for(TeamBackend kind:List.of(TeamBackend.TMUX,TeamBackend.ITERM2,TeamBackend.IN_PROCESS))try{backends.require(kind).close();}catch(RuntimeException ignored){}}
}
