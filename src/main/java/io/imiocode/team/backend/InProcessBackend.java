package io.imiocode.team.backend;

import io.imiocode.team.TeamException;
import io.imiocode.team.model.TeamBackend;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/** 每个成员一条虚拟线程的进程内后端。 */
public final class InProcessBackend implements TeammateBackend {
    private final ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor();private final Map<String,Future<?>> tasks=new ConcurrentHashMap<>();
    @Override public TeamBackend kind(){return TeamBackend.IN_PROCESS;}
    @Override public BackendAvailability probe(Duration timeout){return BackendAvailability.yes();}
    @Override public BackendHandle start(TeammateLaunchRequest request){if(request.inProcessTask()==null)throw new TeamException("in-process 启动任务为空");String id="inproc-"+UUID.randomUUID();FutureTask<Void> future=new FutureTask<>(()->{request.inProcessTask().run();return null;}){@Override protected void done(){tasks.remove(id,this);}};tasks.put(id,future);try{executor.execute(future);}catch(RuntimeException e){tasks.remove(id,future);throw e;}return new BackendHandle(kind(),id);}
    @Override public void wake(BackendHandle handle){String id=require(handle);Future<?> task=tasks.get(id);if(task==null||task.isDone())throw new TeamException("in-process 成员已停止");}
    @Override public void stop(BackendHandle handle,Duration timeout){Future<?> future=tasks.remove(require(handle));if(future!=null)future.cancel(true);}
    private String require(BackendHandle h){if(h.backend()!=kind()||!h.value().startsWith("inproc-"))throw new TeamException("in-process 句柄不属于本后端");return h.value();}
    @Override public void close(){tasks.values().forEach(f->f.cancel(true));tasks.clear();executor.shutdownNow();try{executor.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
