package io.imiocode.team.runtime;

import io.imiocode.team.mailbox.MailboxMessageType;
import io.imiocode.team.model.TeamPrincipal;

/** SendMessage 工具所需的最小团队消息端口。 */
@FunctionalInterface
public interface TeamMessenger {
    SendReceipt send(TeamPrincipal sender, String recipient, MailboxMessageType type,
                     String summary, String body, String taskId);
}
