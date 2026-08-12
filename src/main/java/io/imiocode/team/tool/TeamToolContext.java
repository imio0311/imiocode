package io.imiocode.team.tool;

import io.imiocode.team.TeamException;
import io.imiocode.team.model.TeamPrincipal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Lead 工具间共享当前团队；成员工具直接构造固定 principal。 */
public final class TeamToolContext {
    private final AtomicReference<TeamPrincipal> current=new AtomicReference<>();
    private final AtomicReference<Runnable> onSelected=new AtomicReference<>(()->{});
    private final AtomicReference<Runnable> onCleared=new AtomicReference<>(()->{});
    public Optional<TeamPrincipal> current(){return Optional.ofNullable(current.get());}
    public TeamPrincipal require(){return current().orElseThrow(()->new TeamException("尚未创建或选择团队"));}
    public void select(TeamPrincipal principal){current.set(principal);onSelected.get().run();}
    public void clear(String team){TeamPrincipal removed=current.getAndUpdate(p->p!=null&&p.teamName().equals(team)?null:p);if(removed!=null&&removed.teamName().equals(team))onCleared.get().run();}
    /** 应用装配时绑定工具启停；立即同步当前上下文，避免恢复窗口暴露错误工具集。 */
    public void configureLifecycle(Runnable selected,Runnable cleared){onSelected.set(selected==null?()->{}:selected);onCleared.set(cleared==null?()->{}:cleared);if(current.get()==null)onCleared.get().run();else onSelected.get().run();}
}
