package io.imiocode.hook.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/** config.yaml 中单个 Hook 的原始可空结构。 */
public record HookDocument(String id, String event,
                           @JsonProperty("if") String condition,
                           Boolean once, Boolean async, Boolean reject,
                           @JsonProperty("reject-message") String rejectMessage,
                           @JsonProperty("on-error") String onError,
                           ActionDocument action) { }
