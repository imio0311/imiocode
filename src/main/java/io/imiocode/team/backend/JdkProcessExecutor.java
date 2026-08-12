package io.imiocode.team.backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 只接受参数数组的外部进程执行器。 */
public final class JdkProcessExecutor implements ProcessExecutor {
    @Override public ProcessResult run(Path cwd,List<String> command,Duration timeout){
        if(command==null||command.isEmpty())throw new IllegalArgumentException("外部命令不能为空");
        try{Process process=new ProcessBuilder(List.copyOf(command)).directory(cwd.toFile()).redirectErrorStream(true).start();boolean done=process.waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS);if(!done){process.destroyForcibly();return new ProcessResult(124,"命令超时");}return new ProcessResult(process.exitValue(),new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8).strip());}
        catch(IOException e){return new ProcessResult(127,"命令不可用");}catch(InterruptedException e){Thread.currentThread().interrupt();return new ProcessResult(130,"命令已中断");}
    }
}
