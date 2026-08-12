package io.imiocode.team.backend;

import io.imiocode.team.TeamException;
import io.imiocode.team.model.TeamBackend;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/** 只管理 `imiocode-<team>-<agent>` 命名 pane 的 tmux 后端。 */
public final class TmuxBackend implements TeammateBackend {
    private final ProcessExecutor processes;private final Path cwd;private final Map<String,String> environment;private final Map<String,String> handles=new java.util.concurrent.ConcurrentHashMap<>();
    public TmuxBackend(ProcessExecutor processes,Path cwd){this(processes,cwd,System.getenv());}
    public TmuxBackend(ProcessExecutor processes,Path cwd,Map<String,String> environment){this.processes=processes;this.cwd=cwd;this.environment=Map.copyOf(environment);}
    @Override public TeamBackend kind(){return TeamBackend.TMUX;}
    @Override public BackendAvailability probe(Duration timeout){if(!environment.containsKey("TMUX")||environment.get("TMUX").isBlank())return BackendAvailability.no("当前进程不在 tmux 会话中");ProcessResult r=processes.run(cwd,List.of("tmux","display-message","-p","#{session_id}"),timeout);return r.success()&&!r.output().isBlank()?BackendAvailability.yes():BackendAvailability.no("无法验证当前 tmux 会话");}
    @Override public BackendHandle start(TeammateLaunchRequest req){if(req.command().isEmpty())throw new TeamException("tmux 成员进程命令为空");String title="imiocode-"+req.teamName()+"-"+req.agentId();List<String> cmd=new ArrayList<>(List.of("tmux","split-window","-P","-F","#{pane_id}","-c",req.worktree().toString()));cmd.addAll(req.command());ProcessResult r=processes.run(cwd,cmd,Duration.ofSeconds(10));if(!r.success()||r.output().isBlank())throw new TeamException("tmux pane 启动失败");String pane=r.output().lines().findFirst().orElseThrow().trim();handles.put(pane,title);processes.run(cwd,List.of("tmux","select-pane","-t",pane,"-T",title),Duration.ofSeconds(2));return new BackendHandle(kind(),pane);}
    @Override public void wake(BackendHandle h){String pane=require(h);ProcessResult r=processes.run(cwd,List.of("tmux","send-keys","-t",pane,"IMIO_MAILBOX_WAKE","Enter"),Duration.ofSeconds(2));if(!r.success())throw new TeamException("tmux 唤醒失败");}
    @Override public void stop(BackendHandle h,Duration timeout){String pane=require(h);ProcessResult stopped=processes.run(cwd,List.of("tmux","kill-pane","-t",pane),timeout);if(!stopped.success()){ProcessResult stillPresent=processes.run(cwd,List.of("tmux","display-message","-p","-t",pane,"#{pane_id}"),Duration.ofSeconds(2));if(stillPresent.success())throw new TeamException("tmux pane 停止失败，句柄已保留: "+pane);}handles.remove(pane);}
    private String require(BackendHandle h){if(h.backend()!=kind()||!handles.containsKey(h.value()))throw new TeamException("tmux pane 不属于 ImioCode");return h.value();}
    @Override public void close(){for(String pane:new ArrayList<>(handles.keySet()))try{stop(new BackendHandle(kind(),pane),Duration.ofSeconds(2));}catch(RuntimeException ignored){}}
}
