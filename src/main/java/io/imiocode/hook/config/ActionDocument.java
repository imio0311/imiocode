package io.imiocode.hook.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** 四类 Hook action 的 YAML 联合结构。 */
public record ActionDocument(String type, String command, String message, String url,
                             String method, Map<String, String> headers, String body,
                             String prompt,
                             @JsonProperty("timeout-seconds") Integer timeoutSeconds) { }
