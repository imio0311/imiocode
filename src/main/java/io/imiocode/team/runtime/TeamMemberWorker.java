package io.imiocode.team.runtime;

import io.imiocode.team.mailbox.MailboxMessage;
import io.imiocode.team.mailbox.MailboxMessageType;
import io.imiocode.team.mailbox.MailboxStore;
import io.imiocode.team.model.TeamConfig;
import io.imiocode.team.model.TeammateInfo;
import io.imiocode.team.model.TeammateStatus;
import io.imiocode.team.persistence.TeamStore;
import io.imiocode.team.persistence.TranscriptEntry;
import io.imiocode.team.persistence.TranscriptRole;
import io.imiocode.team.persistence.TranscriptStore;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/** 同时供进程内后端和受限外部成员入口使用的 Mailbox 工作循环。 */
public final class TeamMemberWorker {
    private final TeamStore teams;
    private final MailboxStore mailbox;
    private final TranscriptStore transcripts;
    private final TeamMemberRunner runner;
    private final Clock clock;

    public TeamMemberWorker(TeamStore teams, MailboxStore mailbox,
                            TranscriptStore transcripts, TeamMemberRunner runner) {
        this(teams, mailbox, transcripts, runner, Clock.systemUTC());
    }

    TeamMemberWorker(TeamStore teams, MailboxStore mailbox,
                     TranscriptStore transcripts, TeamMemberRunner runner, Clock clock) {
        this.teams = Objects.requireNonNull(teams);
        this.mailbox = Objects.requireNonNull(mailbox);
        this.transcripts = Objects.requireNonNull(transcripts);
        this.runner = Objects.requireNonNull(runner);
        this.clock = Objects.requireNonNull(clock);
    }

    /** 常驻等待成员 Mailbox；每轮 Agent/LLM 执行结束即由 runner 释放资源。 */
    public void run(String team, String agent, String type, Path worktree) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<MailboxMessage> pending = mailbox.unread(team, agent);
                if (pending.isEmpty()) {
                    if (teams.require(team).members().get(agent).status() != TeammateStatus.IDLE) {
                        markStatus(team, agent, TeammateStatus.IDLE);
                    }
                    Thread.sleep(200);
                    continue;
                }
                for (MailboxMessage message : pending) {
                    if (message.type() == MailboxMessageType.IDLE) {
                        mailbox.acknowledge(team, agent, message.id());
                        continue;
                    }
                    // 崩溃可能发生在回复写盘后、ACK 前；完整回复是幂等提交凭据，不得重复执行消息。
                    if (transcripts.load(team, agent).stream().anyMatch(entry ->
                            entry.role() == TranscriptRole.ASSISTANT
                                    && message.id().equals(entry.correlationId()))) {
                        mailbox.acknowledge(team, agent, message.id());
                        continue;
                    }
                    transcripts.append(team, agent, TranscriptRole.MAILBOX,
                            message.type() + ": " + message.summary(), message.id());
                    if (message.type() == MailboxMessageType.STOP_REQUEST) {
                        mailbox.acknowledge(team, agent, message.id());
                        markStatus(team, agent, TeammateStatus.STOPPED);
                        transcripts.append(team, agent, TranscriptRole.STATUS, "STOPPED", message.id());
                        TeamConfig config = teams.require(team);
                        mailbox.send(team, agent, config.leadAgentId(),
                                MailboxMessageType.STOP_RESPONSE, "成员已停止", "", message.taskId());
                        return;
                    }
                    markStatus(team, agent, TeammateStatus.RUNNING);
                    List<TranscriptEntry> history = transcripts.load(team, agent);
                    transcripts.append(team, agent, TranscriptRole.USER,
                            message.body(), message.id());
                    String output = runner.run(team, agent, type, worktree,
                            message.body(), history);
                    transcripts.append(team, agent, TranscriptRole.ASSISTANT,
                            output, message.id());
                    mailbox.acknowledge(team, agent, message.id());
                    TeamConfig config = teams.require(team);
                    mailbox.send(team, agent, config.leadAgentId(), MailboxMessageType.IDLE,
                            "成员已空闲", output, message.taskId());
                }
                markStatus(team, agent, TeammateStatus.IDLE);
                transcripts.append(team, agent, TranscriptRole.STATUS, "IDLE", "");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException exception) {
                try {
                    transcripts.append(team, agent, TranscriptRole.STATUS, "成员执行失败", "");
                } catch (RuntimeException ignored) {
                    // transcript 本身可能已经损坏或达到上限。
                }
                try {
                    markStatus(team, agent, TeammateStatus.FAILED);
                } catch (RuntimeException ignored) {
                    // 花名册不可写时交给启动恢复流程降级为 stopped。
                }
                try {
                    TeamConfig config = teams.require(team);
                    mailbox.send(team, agent, config.leadAgentId(), MailboxMessageType.MESSAGE,
                            "成员执行失败", "成员已安全停止，请检查 transcript 隔离记录后决定是否续写。", "");
                } catch (RuntimeException ignored) {
                    // Mailbox 也不可写时不暴露原始异常，花名册状态仍可诊断。
                }
                return;
            }
        }
    }

    private void markStatus(String team, String agent, TeammateStatus status) {
        teams.update(team, config -> {
            TeammateInfo member = config.members().get(agent);
            return config.replaceMember(member.withStatus(status, clock.instant()), clock.instant());
        });
    }
}
