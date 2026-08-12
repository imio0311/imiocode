package io.imiocode.team.runtime;
import java.util.List;
public record TeamDeletionReport(boolean deleted,List<String> retained,List<String> warnings) {
    public TeamDeletionReport {retained=List.copyOf(retained);warnings=List.copyOf(warnings);}
}
