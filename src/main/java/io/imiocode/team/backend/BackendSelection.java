package io.imiocode.team.backend;

import java.util.List;

public record BackendSelection(TeammateBackend backend,List<String> warnings) {
    public BackendSelection {warnings=List.copyOf(warnings==null?List.of():warnings);}
}
