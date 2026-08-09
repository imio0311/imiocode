package io.imiocode.permission.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.imiocode.config.ConfigException;
import io.imiocode.permission.PermissionAction;
import io.imiocode.permission.PermissionMode;
import io.imiocode.permission.PermissionRule;
import io.imiocode.permission.PermissionRuleLayer;
import io.imiocode.permission.PermissionSettings;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 在会话启动时一次性加载用户、项目和本地权限规则。 */
public final class PermissionRuleLoader {
    private final ObjectMapper mapper;
    private final PermissionGlobMatcher globMatcher;

    public PermissionRuleLoader() {
        this.mapper = JsonMapper.builder(new YAMLFactory())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        this.globMatcher = new PermissionGlobMatcher();
    }

    public PermissionSettings load(Path workspace, Path userHome) {
        return loadLegacy(workspace, userHome);
    }

    /** 按旧格式加载用户、项目和本地三层权限文件。 */
    public PermissionSettings loadLegacy(Path workspace, Path userHome) {
        Objects.requireNonNull(workspace, "workspace 不能为空");
        Objects.requireNonNull(userHome, "userHome 不能为空");
        Path normalizedWorkspace = workspace.toAbsolutePath().normalize();
        LayerDocument user = read(
                userHome.resolve(".imiocode").resolve("permissions.yaml"),
                PermissionRuleLayer.USER);
        LayerDocument project = read(
                normalizedWorkspace.resolve(".imiocode").resolve("permissions.yaml"),
                PermissionRuleLayer.PROJECT);
        LayerDocument local = read(
                normalizedWorkspace.resolve(".imiocode").resolve("permissions.local.yaml"),
                PermissionRuleLayer.LOCAL);
        PermissionMode mode = firstMode(user, project, local);
        return new PermissionSettings(mode, user.rules(), project.rules(), local.rules());
    }

    /** 加载根 config.yaml 中的统一权限域，不读取任何旧权限文件。 */
    public PermissionSettings loadUnified(PermissionConfigDocument document) {
        Objects.requireNonNull(document, "document 不能为空");
        try {
            LayerDocument project = convert(document, PermissionRuleLayer.PROJECT);
            PermissionMode mode = project.mode() == null ? PermissionMode.AUTO_EDIT : project.mode();
            return new PermissionSettings(mode, List.of(), project.rules(), List.of());
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("config.yaml 的 " + exception.getMessage(), exception);
        }
    }

    private LayerDocument read(Path path, PermissionRuleLayer layer) {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return new LayerDocument(null, List.of());
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) {
            throw new ConfigException("权限配置必须是普通文件: " + safeName(layer));
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            PermissionConfigDocument document =
                    mapper.readValue(reader, PermissionConfigDocument.class);
            if (document == null) {
                return new LayerDocument(null, List.of());
            }
            return convert(document, layer);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ConfigException("权限配置格式错误: " + safeName(layer), exception);
        } catch (IOException exception) {
            throw new ConfigException("无法读取权限配置: " + safeName(layer), exception);
        }
    }

    private LayerDocument convert(
            PermissionConfigDocument document,
            PermissionRuleLayer layer) {
        PermissionMode mode;
        try {
            mode = document.mode() == null ? null : PermissionMode.parse(document.mode());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("permissions.mode 无效", exception);
        }
        List<PermissionRule> rules = new ArrayList<>();
        for (int index = 0; index < document.rules().size(); index++) {
            PermissionConfigDocument.RuleDocument rule = document.rules().get(index);
            try {
                if (rule == null || rule.tool() == null || rule.tool().isBlank()) {
                    throw new IllegalArgumentException("tool 不能为空");
                }
                PermissionAction action = PermissionAction.parse(rule.action());
                globMatcher.compile(rule.tool());
                Optional<String> target = Optional.ofNullable(rule.target())
                        .map(String::trim);
                target.ifPresent(globMatcher::compile);
                rules.add(new PermissionRule(layer, action, rule.tool(), target));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "permissions.rules[" + index + "] 无效",
                        exception);
            }
        }
        return new LayerDocument(mode, List.copyOf(rules));
    }

    private static PermissionMode firstMode(LayerDocument... layers) {
        for (LayerDocument layer : layers) {
            if (layer.mode() != null) {
                return layer.mode();
            }
        }
        return PermissionMode.AUTO_EDIT;
    }

    private static String safeName(PermissionRuleLayer layer) {
        return switch (layer) {
            case USER -> "用户级 permissions.yaml";
            case PROJECT -> "项目级 permissions.yaml";
            case LOCAL -> "本地级 permissions.local.yaml";
        };
    }

    private record LayerDocument(PermissionMode mode, List<PermissionRule> rules) {
    }
}
