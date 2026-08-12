package io.imiocode.team.persistence;

import io.imiocode.team.TeamException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** 小型 JSON 快照的 fsync + 原子替换写入器。 */
final class AtomicJsonFile {
    private AtomicJsonFile() { }
    static void write(Path target,String content){
        Path temporary=null;
        try{
            Files.createDirectories(target.getParent());
            temporary=Files.createTempFile(target.getParent(),".team-",".tmp");
            try(FileChannel channel=FileChannel.open(temporary,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING)){
                var buffer=StandardCharsets.UTF_8.encode(content.endsWith("\n")?content:content+"\n");while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
            }
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException e){throw new TeamException("无法原子写入团队数据",e);}
        finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){}}
    }
}
