package io.imiocode.team.persistence;

import io.imiocode.team.TeamException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 可加锁追加并隔离损坏尾部的 JSONL 基础设施。 */
public final class JsonlLog {
    private final int maxBytes;
    public JsonlLog(int maxBytes){if(maxBytes<=0)throw new IllegalArgumentException("maxBytes 必须为正数");this.maxBytes=maxBytes;}
    public synchronized void append(Path file,String json){byte[] data=(json+"\n").getBytes(StandardCharsets.UTF_8);CrossProcessFileLock.withLock(lockFile(file),()->{try{Files.createDirectories(file.getParent());long size=Files.exists(file)?Files.size(file):0;if(size+data.length>maxBytes)throw new TeamException("JSONL 文件超过大小上限");try(FileChannel channel=FileChannel.open(file,StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.APPEND)){ByteBuffer b=ByteBuffer.wrap(data);while(b.hasRemaining())channel.write(b);channel.force(true);}return null;}catch(IOException e){throw new TeamException("无法追加团队日志",e);}});}
    public synchronized List<String> readRecoveringTail(Path file){return CrossProcessFileLock.withLock(lockFile(file),()->readUnlocked(file));}
    private List<String> readUnlocked(Path file){if(!Files.exists(file))return List.of();try{String raw=Files.readString(file,StandardCharsets.UTF_8);String[] lines=raw.split("\n",-1);List<String> valid=new ArrayList<>();for(int i=0;i<lines.length;i++){String line=lines[i];if(line.isBlank())continue;try{new ObjectMapperHolder().mapper.readTree(line);valid.add(line);}catch(IOException e){boolean tail=true;for(int j=i+1;j<lines.length;j++)if(!lines[j].isBlank()){tail=false;break;}if(!tail)throw new TeamException("JSONL 中部损坏");quarantine(file,raw.substring(offsetOfLine(raw,i)));AtomicJsonFile.write(file,String.join("\n",valid));break;}}return List.copyOf(valid);}catch(IOException e){throw new TeamException("无法读取团队日志",e);}}
    private void quarantine(Path file,String tail)throws IOException{Path q=file.resolveSibling(file.getFileName()+".corrupt-"+ DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneOffset.UTC).format(Instant.now()));Files.writeString(q,tail,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}
    private static Path lockFile(Path file){return file.resolveSibling(file.getFileName()+".lock");}
    private static int offsetOfLine(String raw,int line){int offset=0;for(int i=0;i<line;i++){int next=raw.indexOf('\n',offset);if(next<0)return raw.length();offset=next+1;}return offset;}
    private static final class ObjectMapperHolder{final com.fasterxml.jackson.databind.ObjectMapper mapper=new com.fasterxml.jackson.databind.ObjectMapper();}
}
