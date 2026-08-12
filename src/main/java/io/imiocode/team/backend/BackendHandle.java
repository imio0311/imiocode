package io.imiocode.team.backend;

import io.imiocode.team.model.TeamBackend;
import java.util.Objects;

public record BackendHandle(TeamBackend backend,String value) {
    public BackendHandle {Objects.requireNonNull(backend);if(value==null||value.isBlank())throw new IllegalArgumentException("后端句柄不能为空");value=value.trim();}
}
