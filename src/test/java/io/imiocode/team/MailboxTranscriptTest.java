package io.imiocode.team;

import io.imiocode.team.mailbox.*;
import io.imiocode.team.persistence.*;
import io.imiocode.team.model.*;
import io.imiocode.team.runtime.TeamMemberWorker;
import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MailboxTranscriptTest {
    @TempDir Path temp;
    @Test void mailboxIsDurableRedactedAndIdempotent() throws Exception {
        TeamPaths paths=new TeamPaths(temp);MailboxStore store=new MailboxStore(paths,new SecretRedactor("secret-123"),1000,20,100_000);
        MailboxMessage message=store.send("demo","lead","worker",MailboxMessageType.MESSAGE,"摘要","token=secret-123","");
        assertEquals(1,store.unread("demo","worker").size());assertFalse(store.unread("demo","worker").getFirst().body().contains("secret-123"));
        assertTrue(store.acknowledge("demo","worker",message.id()));assertFalse(store.acknowledge("demo","worker",message.id()));assertTrue(store.unread("demo","worker").isEmpty());
        Files.writeString(paths.mailboxFile("demo","worker"),"{broken",StandardOpenOption.APPEND);assertEquals(1,store.readAll("demo","worker").size());
        try(var files=Files.list(paths.mailboxFile("demo","worker").getParent())){assertTrue(files.anyMatch(p->p.getFileName().toString().contains(".corrupt-")));}
    }
    @Test void concurrentStoresDoNotLoseMessagesAndAcknowledgeExactlyOnce() throws Exception {
        TeamPaths paths=new TeamPaths(temp);MailboxStore first=new MailboxStore(paths,new SecretRedactor("secret"),1000,100,200_000);MailboxStore second=new MailboxStore(paths,new SecretRedactor("secret"),1000,100,200_000);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            var futures=new ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<30;i++){int index=i;MailboxStore target=i%2==0?first:second;futures.add(executor.submit(()->target.send("demo","lead","worker",MailboxMessageType.MESSAGE,"m"+index,"body","")));}
            for(var future:futures)future.get();
        }
        assertEquals(30,first.unread("demo","worker").size());String id=first.unread("demo","worker").getFirst().id();
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            var a=executor.submit(()->first.acknowledge("demo","worker",id));var b=executor.submit(()->second.acknowledge("demo","worker",id));
            assertEquals(1,java.util.stream.Stream.of(a.get(),b.get()).filter(Boolean::booleanValue).count());
        }
    }
    @Test void broadcastCreatesIndependentRecipientRecords(){TeamPaths paths=new TeamPaths(temp);MailboxStore store=new MailboxStore(paths,new SecretRedactor("secret"),1000,20,100_000);var sent=store.broadcast("demo","lead",java.util.Set.of("lead","a","b"),MailboxMessageType.MESSAGE,"all","body","");assertEquals(2,sent.size());assertEquals(1,store.unread("demo","a").size());assertEquals(1,store.unread("demo","b").size());assertNotEquals(store.unread("demo","a").getFirst().id(),store.unread("demo","b").getFirst().id());}
    @Test void transcriptRestoresOrderedHistoryAndRedacts(){TeamPaths paths=new TeamPaths(temp);TranscriptStore store=new TranscriptStore(paths,new SecretRedactor("secret-123"),1000,100_000);store.append("demo","worker",TranscriptRole.USER,"hello secret-123","");store.append("demo","worker",TranscriptRole.ASSISTANT,"world","");var history=store.load("demo","worker");assertEquals(2,history.size());assertEquals(TranscriptRole.USER,history.getFirst().role());assertFalse(history.getFirst().content().contains("secret-123"));}
    @Test void unsafeTranscriptFailureStopsMemberAndNotifiesLead() throws Exception {
        TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);Instant now=Instant.now();
        Path memberWorktree=temp.resolve(".imiocode/worktrees/worker");Files.createDirectories(memberWorktree);
        var members=new LinkedHashMap<String,TeammateInfo>();
        members.put("lead",new TeammateInfo("lead","Lead","lead","",TeamBackend.IN_PROCESS,"lead",temp,TeammateStatus.RUNNING,false,now));
        members.put("worker",new TeammateInfo("worker","worker","general-purpose","",TeamBackend.IN_PROCESS,"handle",memberWorktree,TeammateStatus.RUNNING,false,now));
        teams.create(new TeamConfig(1,"demo","","lead",TeamBackend.IN_PROCESS,members,now,now));
        MailboxStore mailbox=new MailboxStore(paths,new SecretRedactor("secret"),1000,20,100_000);
        TranscriptStore transcripts=new TranscriptStore(paths,new SecretRedactor("secret"),1000,100_000);
        transcripts.append("demo","worker",TranscriptRole.USER,"valid","");
        Files.writeString(paths.transcriptFile("demo","worker"),"{broken}\n",StandardOpenOption.APPEND);
        transcripts.append("demo","worker",TranscriptRole.USER,"later","");
        mailbox.send("demo","lead","worker",MailboxMessageType.TASK,"run","prompt","");

        new TeamMemberWorker(teams,mailbox,transcripts,(team,agent,type,worktree,prompt,history)->"never")
                .run("demo","worker","general-purpose",memberWorktree);

        assertEquals(TeammateStatus.FAILED,teams.require("demo").members().get("worker").status());
        assertTrue(mailbox.unread("demo","lead").stream().anyMatch(message->message.summary().equals("成员执行失败")));
    }
    @Test void replyPersistedBeforeCrashMakesMailboxReplayIdempotent() {
        TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);Instant now=Instant.now();Path memberWorktree=temp.resolve(".imiocode/worktrees/worker");
        var members=new LinkedHashMap<String,TeammateInfo>();members.put("lead",new TeammateInfo("lead","Lead","lead","",TeamBackend.IN_PROCESS,"lead",temp,TeammateStatus.RUNNING,false,now));members.put("worker",new TeammateInfo("worker","worker","general-purpose","",TeamBackend.IN_PROCESS,"handle",memberWorktree,TeammateStatus.RUNNING,false,now));teams.create(new TeamConfig(1,"demo","","lead",TeamBackend.IN_PROCESS,members,now,now));
        MailboxStore mailbox=new MailboxStore(paths,new SecretRedactor("secret"),1000,20,100_000);TranscriptStore transcripts=new TranscriptStore(paths,new SecretRedactor("secret"),1000,100_000);var message=mailbox.send("demo","lead","worker",MailboxMessageType.TASK,"run","prompt","");transcripts.append("demo","worker",TranscriptRole.ASSISTANT,"already completed",message.id());mailbox.send("demo","lead","worker",MailboxMessageType.STOP_REQUEST,"stop","","");AtomicInteger calls=new AtomicInteger();
        new TeamMemberWorker(teams,mailbox,transcripts,(team,agent,type,worktree,prompt,history)->{calls.incrementAndGet();return "duplicate";}).run("demo","worker","general-purpose",memberWorktree);
        assertEquals(0,calls.get());assertTrue(mailbox.readAll("demo","worker").stream().filter(item->item.id().equals(message.id())).findFirst().orElseThrow().consumedAt()!=null);
    }
}
