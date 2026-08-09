package io.imiocode.subagent.model;

import java.util.Optional;

public record ModelResolution(String model, Optional<String> warning) {
    public ModelResolution {
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model 不能为空");
        model = model.trim();
        warning = warning == null ? Optional.empty() : warning;
    }
}
