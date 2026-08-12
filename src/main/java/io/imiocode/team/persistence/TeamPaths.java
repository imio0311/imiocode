package io.imiocode.team.persistence;

import io.imiocode.team.TeamException;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;

/** 集中执行团队 slug 与真实路径边界检查。 */
public final class TeamPaths {
    private static final Pattern SLUG=Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?");
    private final Path repositoryRoot,teamsRoot;
    public TeamPaths(Path repositoryRoot){this.repositoryRoot=repositoryRoot.toAbsolutePath().normalize();this.teamsRoot=this.repositoryRoot.resolve(".imiocode").resolve("teams").normalize();if(!teamsRoot.startsWith(this.repositoryRoot))throw new TeamException("团队目录越界");}
    public String requireSlug(String value,String label){if(value==null||!SLUG.matcher(value).matches())throw new IllegalArgumentException(label+" 必须是 1-64 位小写字母、数字或中划线 slug");return value;}
    public Path teamsRoot(){return teamsRoot;}
    public Path repositoryRoot(){return repositoryRoot;}
    public Path teamDirectory(String team){return checked(teamsRoot.resolve(requireSlug(team,"team_name")));}
    public Path mailboxFile(String team,String agent){Path directory=checked(teamDirectory(team).resolve("mailbox"));verifyExisting(directory);return checked(directory.resolve(requireSlug(agent,"agent_id")+".jsonl"));}
    public Path transcriptFile(String team,String agent){Path directory=checked(teamDirectory(team).resolve("transcripts"));verifyExisting(directory);return checked(directory.resolve(requireSlug(agent,"agent_id")+".jsonl"));}
    public Path lockFile(String team,String scope){requireSlug(team,"team_name");requireSlug(scope,"lock_scope");return checked(teamsRoot.resolve(team+"."+scope+".lock"));}
    public void ensureRoot(){try{Files.createDirectories(teamsRoot);if(!teamsRoot.toRealPath().startsWith(repositoryRoot.toRealPath()))throw new TeamException("团队目录真实路径越界");}catch(IOException e){throw new TeamException("无法创建团队目录",e);}}
    public void verifyExisting(Path path){Path n=checked(path);try{if(Files.exists(n,LinkOption.NOFOLLOW_LINKS)&&!n.toRealPath().startsWith(teamsRoot.toRealPath()))throw new TeamException("团队路径真实地址越界");}catch(IOException e){throw new TeamException("无法验证团队路径",e);}}
    public void verifyWorktree(Path worktree,boolean lead){Path n=worktree.toAbsolutePath().normalize();Path managed=repositoryRoot.resolve(".imiocode").normalize();if(lead){if(!n.equals(repositoryRoot))throw new TeamException("Lead 工作区必须是当前仓库");return;}if(!n.startsWith(managed)||n.startsWith(teamsRoot))throw new TeamException("成员 Worktree 必须位于当前仓库 .imiocode 受管目录");try{if(Files.exists(n,LinkOption.NOFOLLOW_LINKS)&&!n.toRealPath().startsWith(managed.toRealPath()))throw new TeamException("成员 Worktree 真实路径越界");}catch(IOException e){throw new TeamException("无法验证成员 Worktree",e);}}
    private Path checked(Path path){Path n=path.toAbsolutePath().normalize();if(!n.startsWith(teamsRoot))throw new TeamException("团队路径越界");return n;}
}
