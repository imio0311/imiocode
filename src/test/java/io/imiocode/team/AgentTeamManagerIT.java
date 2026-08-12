package io.imiocode.team;

import io.imiocode.team.backend.*;
import io.imiocode.team.config.TeamRuntimeConfig;
import io.imiocode.team.mailbox.*;
import io.imiocode.team.model.*;
import io.imiocode.team.persistence.*;
import io.imiocode.team.runtime.*;
import io.imiocode.team.task.TeamTaskStore;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class AgentTeamManagerIT {
    @TempDir Path temp;
    @Test void createsMemberIdlesAndResumesWithSameTranscript() throws Exception {
        assumeGit();run("git","init");run("git","config","user.email","test@example.com");run("git","config","user.name","Test");Files.writeString(temp.resolve("README.md"),"demo\n");run("git","add","README.md");run("git","commit","-m","init");
        TeamRuntimeConfig config=TeamRuntimeConfig.defaults();TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);SecretRedactor redactor=new SecretRedactor("test-secret");MailboxStore mailbox=new MailboxStore(paths,redactor,2000,100,100_000);TranscriptStore transcript=new TranscriptStore(paths,redactor,2000,100_000);TeamTaskStore tasks=new TeamTaskStore(paths,20);InProcessBackend backend=new InProcessBackend();BackendSelector selector=new BackendSelector(List.of(backend),Map.of(),"Windows",Duration.ofMillis(100));AtomicInteger calls=new AtomicInteger();AtomicInteger resumedHistory=new AtomicInteger();
        try(WorktreeManager worktrees=new WorktreeManager(temp,WorktreeConfig.defaults());AgentTeamManager manager=new AgentTeamManager(temp,config,paths,teams,mailbox,transcript,tasks,selector,worktrees,(team,id,type,worktree,prompt,history)->{int call=calls.incrementAndGet();if(call==2)resumedHistory.set((int)history.stream().filter(entry->entry.role()==TranscriptRole.USER||entry.role()==TranscriptRole.ASSISTANT).count());return "reply-"+call+":"+prompt;})){
            TeamConfig created=manager.createTeam(new TeamCreateRequest("demo","","lead","Lead","lead",TeamBackend.IN_PROCESS));TeamPrincipal lead=new TeamPrincipal("demo",created.leadAgentId(),TeamRole.LEAD);
            TeammateInfo member=manager.spawn(lead,new TeammateSpawnRequest("worker","general-purpose","",TeamBackend.IN_PROCESS,false,"first"));waitFor(()->teams.require("demo").members().get("worker").status()==TeammateStatus.IDLE);assertEquals(1,calls.get());assertTrue(Files.isDirectory(member.worktree()));
            SendReceipt receipt=manager.send(lead,"worker",MailboxMessageType.MESSAGE,"follow-up","second","");assertEquals(List.of("worker"),receipt.resumedAgentIds());waitFor(()->calls.get()==2&&teams.require("demo").members().get("worker").status()==TeammateStatus.IDLE);assertEquals(2,resumedHistory.get());ConvergenceReport convergence=manager.converge(lead);assertTrue(convergence.converged());assertTrue(convergence.memberResults().stream().anyMatch(item->item.contains("reply-2:second")));assertEquals(1,convergence.worktrees().size());
            TeamPrincipal worker=new TeamPrincipal("demo","worker",TeamRole.MEMBER);assertThrows(TeamException.class,()->manager.send(worker,"lead",MailboxMessageType.PLAN_APPROVAL,"APPROVED","ok",""));assertThrows(TeamException.class,()->manager.send(lead,"worker",MailboxMessageType.PLAN_APPROVAL,"REJECTED","",""));manager.send(worker,"lead",MailboxMessageType.MESSAGE,"result","unread-important","");TeamDeletionReport refused=manager.deleteTeam(lead,false);assertFalse(refused.deleted());assertTrue(refused.warnings().stream().anyMatch(item->item.contains("Mailbox")));TeamDeletionReport deletion=manager.deleteTeam(lead,true);assertTrue(deletion.deleted());assertTrue(teams.find("demo").isEmpty());
        }
    }
    @Test void spawnFailureRollsBackRosterBackendAndWorktree() throws Exception {
        assumeGit();run("git","init");run("git","config","user.email","test@example.com");run("git","config","user.name","Test");Files.writeString(temp.resolve("README.md"),"demo\n");run("git","add","README.md");run("git","commit","-m","init");
        TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);MailboxStore mailbox=new MailboxStore(paths,new SecretRedactor("secret"),4,20,100_000);TranscriptStore transcript=new TranscriptStore(paths,new SecretRedactor("secret"),1000,100_000);TeamTaskStore tasks=new TeamTaskStore(paths,20);InProcessBackend backend=new InProcessBackend();BackendSelector selector=new BackendSelector(List.of(backend),Map.of(),"Windows",Duration.ofMillis(100));
        try(WorktreeManager worktrees=new WorktreeManager(temp,WorktreeConfig.defaults());AgentTeamManager manager=new AgentTeamManager(temp,TeamRuntimeConfig.defaults(),paths,teams,mailbox,transcript,tasks,selector,worktrees,(team,id,type,worktree,prompt,history)->"reply")){
            TeamConfig created=manager.createTeam(new TeamCreateRequest("demo","","lead","Lead","lead",TeamBackend.IN_PROCESS));TeamPrincipal lead=new TeamPrincipal("demo",created.leadAgentId(),TeamRole.LEAD);
            assertThrows(IllegalArgumentException.class,()->manager.spawn(lead,new TeammateSpawnRequest("worker","general-purpose","",TeamBackend.IN_PROCESS,false,"prompt-too-long")));
            assertEquals(Set.of("lead"),teams.require("demo").members().keySet());assertTrue(worktrees.list().isEmpty());
        }
    }
    @Test void wakeFailureDoesNotBlockDeleteAfterMemberStopsByPolling() throws Exception {
        assumeGit();run("git","init");run("git","config","user.email","test@example.com");run("git","config","user.name","Test");Files.writeString(temp.resolve("README.md"),"demo\n");run("git","add","README.md");run("git","commit","-m","init");
        TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);MailboxStore mailbox=new MailboxStore(paths,new SecretRedactor("secret"),2000,100,100_000);TranscriptStore transcript=new TranscriptStore(paths,new SecretRedactor("secret"),2000,100_000);TeamTaskStore tasks=new TeamTaskStore(paths,20);WakeFailingBackend backend=new WakeFailingBackend();BackendSelector selector=new BackendSelector(List.of(backend),Map.of(),"Windows",Duration.ofMillis(100));
        try(WorktreeManager worktrees=new WorktreeManager(temp,WorktreeConfig.defaults());AgentTeamManager manager=new AgentTeamManager(temp,TeamRuntimeConfig.defaults(),paths,teams,mailbox,transcript,tasks,selector,worktrees,(team,id,type,worktree,prompt,history)->"reply")){
            TeamConfig created=manager.createTeam(new TeamCreateRequest("demo","","lead","Lead","lead",TeamBackend.IN_PROCESS));TeamPrincipal lead=new TeamPrincipal("demo",created.leadAgentId(),TeamRole.LEAD);manager.spawn(lead,new TeammateSpawnRequest("worker","general-purpose","",TeamBackend.IN_PROCESS,false,"first"));waitFor(()->teams.require("demo").members().get("worker").status()==TeammateStatus.IDLE);
            TeamDeletionReport deleted=manager.deleteTeam(lead,true);assertTrue(deleted.deleted());assertTrue(deleted.warnings().stream().anyMatch(item->item.contains("唤醒失败")));assertTrue(teams.find("demo").isEmpty());
        }
    }
    @Test void dirtyMemberWorktreeIsRetainedUntilDiscardAndUnrelatedWorktreeSurvives() throws Exception {
        assumeGit();run("git","init");run("git","config","user.email","test@example.com");run("git","config","user.name","Test");Files.writeString(temp.resolve("README.md"),"demo\n");run("git","add","README.md");run("git","commit","-m","init");
        TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);MailboxStore mailbox=new MailboxStore(paths,new SecretRedactor("secret"),2000,100,100_000);TranscriptStore transcript=new TranscriptStore(paths,new SecretRedactor("secret"),2000,100_000);TeamTaskStore tasks=new TeamTaskStore(paths,20);InProcessBackend backend=new InProcessBackend();BackendSelector selector=new BackendSelector(List.of(backend),Map.of(),"Windows",Duration.ofMillis(100));
        try(WorktreeManager worktrees=new WorktreeManager(temp,WorktreeConfig.defaults())){
            var unrelated=worktrees.create("unrelated-demo");
            try(AgentTeamManager manager=new AgentTeamManager(temp,TeamRuntimeConfig.defaults(),paths,teams,mailbox,transcript,tasks,selector,worktrees,(team,id,type,worktree,prompt,history)->"reply")){
                TeamConfig created=manager.createTeam(new TeamCreateRequest("demo","","lead","Lead","lead",TeamBackend.IN_PROCESS));TeamPrincipal lead=new TeamPrincipal("demo",created.leadAgentId(),TeamRole.LEAD);
                TeammateInfo member=manager.spawn(lead,new TeammateSpawnRequest("worker","general-purpose","",TeamBackend.IN_PROCESS,false,"first"));waitFor(()->teams.require("demo").members().get("worker").status()==TeammateStatus.IDLE);Files.writeString(member.worktree().resolve("result.txt"),"valuable");
                TeamDeletionReport retained=manager.deleteTeam(lead,false);assertFalse(retained.deleted());assertTrue(retained.retained().contains(member.worktree().toString()));assertTrue(Files.exists(member.worktree().resolve("result.txt")));assertTrue(Files.exists(unrelated.session().worktreePath()));
                assertTrue(manager.deleteTeam(lead,true).deleted());assertFalse(Files.exists(member.worktree()));assertTrue(Files.exists(unrelated.session().worktreePath()));
            }finally{worktrees.remove(unrelated.session().slug(),true);}
        }
    }
    private void assumeGit(){try{Process p=new ProcessBuilder("git","--version").start();Assumptions.assumeTrue(p.waitFor()==0);}catch(Exception e){Assumptions.assumeTrue(false,"git 不可用");}}
    private void run(String...command)throws Exception{Process p=new ProcessBuilder(command).directory(temp.toFile()).redirectErrorStream(true).start();String output=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertEquals(0,p.waitFor(),output);}
    private static void waitFor(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+Duration.ofSeconds(10).toNanos();while(System.nanoTime()<end){if(condition.getAsBoolean())return;Thread.sleep(25);}fail("等待团队状态超时");}
    private static final class WakeFailingBackend implements TeammateBackend {
        private final InProcessBackend delegate=new InProcessBackend();
        @Override public TeamBackend kind(){return TeamBackend.IN_PROCESS;}
        @Override public BackendAvailability probe(Duration timeout){return BackendAvailability.yes();}
        @Override public BackendHandle start(TeammateLaunchRequest request){return delegate.start(request);}
        @Override public void wake(BackendHandle handle){throw new TeamException("模拟唤醒失败");}
        @Override public void stop(BackendHandle handle,Duration timeout){delegate.stop(handle,timeout);}
        @Override public void close(){delegate.close();}
    }
}
