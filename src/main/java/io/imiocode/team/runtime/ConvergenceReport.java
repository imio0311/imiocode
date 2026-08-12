package io.imiocode.team.runtime;
import java.util.List;
public record ConvergenceReport(String teamName,boolean converged,List<String> memberStates,
                                List<String> memberResults,List<String> taskStates,
                                List<String> unreadMail,List<String> worktrees) {
    public ConvergenceReport {
        memberStates=List.copyOf(memberStates);memberResults=List.copyOf(memberResults);
        taskStates=List.copyOf(taskStates);unreadMail=List.copyOf(unreadMail);worktrees=List.copyOf(worktrees);
    }
}
