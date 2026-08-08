package io.imiocode.skill;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Set;
import java.util.LinkedHashSet;

/** 三级来源加载、同名覆盖和原子热刷新的统一入口。 */
public final class SkillLoader {
    private static final Set<String> RESERVED_COMMANDS = Set.of(
            "help", "compact", "clear", "plan", "do", "session", "memory",
            "permission", "status", "skill", "skills", "exit", "quit",
            "verbose", "compact-ui");
    private final Path projectRoot;
    private final Path userRoot;
    private final ClassLoader classLoader;
    private final SkillParser parser;
    private final AtomicReference<SkillCatalogSnapshot> current =
            new AtomicReference<>(SkillCatalogSnapshot.empty());
    private String fingerprint = "";

    public SkillLoader(Path workspace, Path userHome) {
        this(workspace.resolve(".imiocode").resolve("skills"),
                userHome.resolve(".imiocode").resolve("skills"),
                Thread.currentThread().getContextClassLoader(), new SkillParser());
    }

    public SkillLoader(Path projectRoot, Path userRoot, ClassLoader classLoader, SkillParser parser) {
        this.projectRoot = normalize(projectRoot);
        this.userRoot = normalize(userRoot);
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        this.parser = Objects.requireNonNull(parser, "parser");
        reload();
    }

    public synchronized SkillCatalogSnapshot snapshot() {
        String next = fingerprint();
        if (!next.equals(fingerprint)) reloadInternal(next, false);
        return current.get();
    }

    public synchronized SkillCatalogSnapshot reload() {
        return reloadInternal(fingerprint(), true);
    }

    public LoadedSkill load(String name, String arguments) {
        SkillDescriptor descriptor = snapshot().find(name)
                .orElseThrow(() -> new SkillException("未知 Skill: " + name));
        return parser.parseLoaded(descriptor, arguments);
    }

    private SkillCatalogSnapshot reloadInternal(String nextFingerprint, boolean forced) {
        SkillCatalogSnapshot previous = current.get();
        List<String> diagnostics = new ArrayList<>();
        LinkedHashMap<String, SkillDescriptor> merged = new LinkedHashMap<>();
        loadBuiltins(merged, diagnostics);
        loadDirectory(userRoot, SkillOrigin.USER, merged, diagnostics);
        loadDirectory(projectRoot, SkillOrigin.PROJECT, merged, diagnostics);
        merged = validateCommandNames(merged, diagnostics);
        if (!diagnostics.isEmpty() && previous.generation() > 0) {
            return new SkillCatalogSnapshot(previous.generation(), previous.skills(), diagnostics);
        }
        SkillCatalogSnapshot next = new SkillCatalogSnapshot(
                previous.generation() + 1, merged, diagnostics);
        current.set(next);
        fingerprint = nextFingerprint;
        return next;
    }

    private static LinkedHashMap<String, SkillDescriptor> validateCommandNames(
            Map<String, SkillDescriptor> candidates, List<String> diagnostics) {
        LinkedHashMap<String, SkillDescriptor> valid = new LinkedHashMap<>();
        LinkedHashSet<String> occupied = new LinkedHashSet<>(RESERVED_COMMANDS);
        candidates.values().stream()
                .sorted(Comparator
                        .comparingInt((SkillDescriptor value) -> value.origin().priority()).reversed()
                        .thenComparing(value -> value.metadata().name()))
                .forEach(skill -> {
                    LinkedHashSet<String> names = new LinkedHashSet<>();
                    names.add(skill.metadata().name());
                    names.addAll(skill.metadata().aliases());
                    List<String> conflicts = names.stream().filter(occupied::contains).sorted().toList();
                    if (!conflicts.isEmpty()) {
                        diagnostics.add("[" + skill.origin() + "] Skill 命令名或别名冲突: "
                                + String.join(", ", conflicts));
                        return;
                    }
                    occupied.addAll(names);
                    valid.put(skill.metadata().name(), skill);
                });
        return valid;
    }

    private void loadBuiltins(Map<String, SkillDescriptor> target, List<String> diagnostics) {
        try (InputStream stream = classLoader.getResourceAsStream("skills/index.txt")) {
            if (stream == null) return;
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String line : text.lines().map(String::trim).filter(value -> !value.isEmpty()).toList()) {
                try {
                    SkillDescriptor descriptor = parser.parseDescriptor(
                            new ClasspathSkillSource(classLoader, line), SkillOrigin.BUILTIN);
                    target.put(descriptor.metadata().name(), descriptor);
                } catch (RuntimeException exception) {
                    diagnostics.add("[内置] " + line + "：" + safe(exception));
                }
            }
        } catch (IOException exception) {
            diagnostics.add("无法读取内置 Skill 索引");
        }
    }

    private void loadDirectory(Path root, SkillOrigin origin,
                               Map<String, SkillDescriptor> target, List<String> diagnostics) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (var entries = Files.list(root)) {
            for (Path path : entries.sorted().toList()) {
                boolean directory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
                boolean markdown = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                        && path.getFileName().toString().toLowerCase().endsWith(".md");
                if (!directory && !markdown) continue;
                if (Files.isSymbolicLink(path)) {
                    diagnostics.add("[" + origin + "] 不允许符号链接: " + path.getFileName());
                    continue;
                }
                if (directory && !Files.isRegularFile(path.resolve("SKILL.md"), LinkOption.NOFOLLOW_LINKS)) continue;
                try {
                    SkillDescriptor descriptor = parser.parseDescriptor(new FileSkillSource(path), origin);
                    target.put(descriptor.metadata().name(), descriptor);
                } catch (RuntimeException exception) {
                    diagnostics.add("[" + origin + "] " + path.getFileName() + "：" + safe(exception));
                }
            }
        } catch (IOException exception) {
            diagnostics.add("无法扫描 Skill 目录: " + root);
        }
    }

    private String fingerprint() {
        StringBuilder value = new StringBuilder();
        appendFingerprint(projectRoot, value);
        appendFingerprint(userRoot, value);
        return Integer.toHexString(value.toString().hashCode());
    }

    private static void appendFingerprint(Path root, StringBuilder value) {
        value.append(root).append('|');
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                value.append(root.relativize(path)).append(':');
                try {
                    BasicFileAttributes attrs = Files.readAttributes(
                            path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    value.append(attrs.size()).append(':').append(attrs.lastModifiedTime().toMillis());
                } catch (IOException exception) {
                    value.append("unavailable");
                }
                value.append(';');
            }
        } catch (IOException exception) {
            value.append("scan-error");
        }
    }

    private static String safe(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Skill 配置无效" : message;
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
