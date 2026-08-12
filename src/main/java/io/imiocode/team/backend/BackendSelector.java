package io.imiocode.team.backend;

import io.imiocode.team.TeamException;
import io.imiocode.team.model.TeamBackend;
import java.time.Duration;
import java.util.*;

/** 推荐 auto 顺序：tmux 环境、macOS iTerm2、最后 in-process。 */
public final class BackendSelector {
    private final Map<TeamBackend,TeammateBackend> backends;private final Map<String,String> environment;private final String os;private final Duration timeout;
    public BackendSelector(Collection<? extends TeammateBackend> backends,Map<String,String> environment,String os,Duration timeout){Map<TeamBackend,TeammateBackend> map=new EnumMap<>(TeamBackend.class);for(TeammateBackend b:backends)map.put(b.kind(),b);this.backends=Map.copyOf(map);this.environment=Map.copyOf(environment);this.os=os==null?"":os.toLowerCase(Locale.ROOT);this.timeout=timeout;}
    public BackendSelection select(TeamBackend requested){if(requested!=TeamBackend.AUTO){TeammateBackend b=require(requested);BackendAvailability a=b.probe(timeout);if(!a.available())throw new TeamException("显式后端不可用: "+requested.configValue()+" ("+a.diagnostic()+")");return new BackendSelection(b,List.of());}List<TeamBackend> order=new ArrayList<>();if(environment.containsKey("TMUX"))order.add(TeamBackend.TMUX);if(os.contains("mac"))order.add(TeamBackend.ITERM2);order.add(TeamBackend.IN_PROCESS);List<String>warnings=new ArrayList<>();for(TeamBackend kind:order){TeammateBackend b=backends.get(kind);if(b==null)continue;BackendAvailability a=b.probe(timeout);if(a.available())return new BackendSelection(b,warnings);warnings.add(kind.configValue()+" 不可用: "+a.diagnostic());}throw new TeamException("没有可用的成员后端");}
    public BackendHandle start(TeamBackend requested,TeammateLaunchRequest launch,List<String> warnings){BackendSelection selection=select(requested);warnings.addAll(selection.warnings());try{return selection.backend().start(launch);}catch(RuntimeException first){if(requested!=TeamBackend.AUTO||selection.backend().kind()==TeamBackend.IN_PROCESS)throw first;warnings.add(selection.backend().kind().configValue()+" 启动失败，已回退 in-process");TeammateBackend fallback=require(TeamBackend.IN_PROCESS);if(!fallback.probe(timeout).available())throw first;return fallback.start(launch);}}
    public TeammateBackend require(TeamBackend kind){TeammateBackend b=backends.get(kind);if(b==null)throw new TeamException("后端未注册: "+kind.configValue());return b;}
}
