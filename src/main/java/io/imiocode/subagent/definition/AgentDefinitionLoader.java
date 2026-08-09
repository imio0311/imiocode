package io.imiocode.subagent.definition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** 插件 -> 内置 -> 用户 -> 项目逐层覆盖，非法高层定义不遮蔽低层有效定义。 */
public final class AgentDefinitionLoader {
    private final Path projectRoot;
    private final Path userRoot;
    private final List<Path> pluginRoots;
    private final ClassLoader classLoader;
    private final AgentDefinitionParser parser;
    private final AtomicReference<AgentCatalogSnapshot> current = new AtomicReference<>(AgentCatalogSnapshot.empty());

    public AgentDefinitionLoader(Path workspace, Path userHome) {
        this(workspace.resolve(".imiocode/agents"), userHome.resolve(".imiocode/agents"),
                discoverPluginRoots(workspace, userHome),
                Thread.currentThread().getContextClassLoader(), new AgentDefinitionParser());
    }
    public AgentDefinitionLoader(Path projectRoot, Path userRoot, List<Path> pluginRoots,
                                 ClassLoader classLoader, AgentDefinitionParser parser) {
        this.projectRoot = normalize(projectRoot); this.userRoot = normalize(userRoot);
        this.pluginRoots = Objects.requireNonNullElse(pluginRoots, List.<Path>of()).stream().map(AgentDefinitionLoader::normalize).toList();
        this.classLoader = Objects.requireNonNull(classLoader); this.parser = Objects.requireNonNull(parser);
        reload();
    }
    public AgentCatalogSnapshot snapshot() { return current.get(); }
    public synchronized AgentCatalogSnapshot reload() {
        Map<String, AgentDefinition> merged = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        for (Path root : pluginRoots) loadDirectory(root, AgentDefinitionSource.PLUGIN, merged, diagnostics);
        loadBuiltins(merged, diagnostics);
        loadDirectory(userRoot, AgentDefinitionSource.USER, merged, diagnostics);
        loadDirectory(projectRoot, AgentDefinitionSource.PROJECT, merged, diagnostics);
        AgentCatalogSnapshot next = new AgentCatalogSnapshot(current.get().generation() + 1, merged, diagnostics);
        current.set(next); return next;
    }
    public AgentDefinition require(String name) {
        return snapshot().find(name).orElseThrow(() -> new AgentDefinitionException("未知 Agent: " + name));
    }

    private void loadBuiltins(Map<String, AgentDefinition> target, List<String> diagnostics) {
        try (InputStream index = classLoader.getResourceAsStream("agents/index.txt")) {
            if (index == null) { diagnostics.add("无法读取内置 Agent 索引"); return; }
            for (String resource : new String(index.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim).filter(v -> !v.isEmpty() && !v.startsWith("#")).toList()) {
                try (InputStream stream = classLoader.getResourceAsStream("agents/" + resource)) {
                    if (stream == null) throw new IOException("资源不存在");
                    AgentDefinition definition = parser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                            AgentDefinitionSource.BUILTIN, null);
                    target.put(definition.name(), definition);
                } catch (RuntimeException | IOException exception) {
                    diagnostics.add("[BUILTIN] " + resource + ": " + safe(exception));
                }
            }
        } catch (IOException exception) { diagnostics.add("无法读取内置 Agent 索引"); }
    }
    private void loadDirectory(Path root, AgentDefinitionSource source,
                               Map<String, AgentDefinition> target, List<String> diagnostics) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.list(root)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md")).sorted().toList()) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
                    diagnostics.add("[" + source + "] 不允许符号链接: " + path.getFileName()); continue;
                }
                try {
                    AgentDefinition definition = parser.parse(Files.readString(path, StandardCharsets.UTF_8), source, path);
                    target.put(definition.name(), definition);
                } catch (RuntimeException | IOException exception) {
                    diagnostics.add("[" + source + "] " + path.getFileName() + ": " + safe(exception));
                }
            }
        } catch (IOException exception) { diagnostics.add("无法扫描 Agent 目录: " + root); }
    }
    private static Path normalize(Path path) { return Objects.requireNonNull(path).toAbsolutePath().normalize(); }
    private static List<Path> discoverPluginRoots(Path workspace, Path userHome) {
        List<Path> result = new ArrayList<>();
        for (Path plugins : List.of(userHome.resolve(".imiocode/plugins"), workspace.resolve(".imiocode/plugins"))) {
            if (!Files.isDirectory(plugins, LinkOption.NOFOLLOW_LINKS)) continue;
            try (var paths = Files.list(plugins)) {
                paths.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                        .map(path -> path.resolve("agents"))
                        .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                        .sorted().forEach(result::add);
            } catch (IOException ignored) { }
        }
        return List.copyOf(result);
    }
    private static String safe(Throwable exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? "Agent 配置无效" : exception.getMessage();
    }
}
