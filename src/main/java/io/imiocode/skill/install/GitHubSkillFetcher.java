package io.imiocode.skill.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 使用 Raw 与 GitHub Contents API 获取自包含 Skill 包。 */
public final class GitHubSkillFetcher implements RemoteSkillFetcher {
    private static final int MAX_DEPTH = 8;
    private final SkillRemoteTransport transport;
    private final ObjectMapper json;

    public GitHubSkillFetcher(SkillRemoteTransport transport) {
        this(transport, new ObjectMapper());
    }

    GitHubSkillFetcher(SkillRemoteTransport transport, ObjectMapper json) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public RemoteSkillPackage fetch(RemoteSkillLocation location, SkillDownloadBudget budget) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(budget, "budget");
        if (location.kind() == RemoteSkillKind.GITHUB_RAW) {
            byte[] body = fetchFile(location, location.path(), budget);
            return new RemoteSkillPackage(location.sourceUri(),
                    List.of(new RemoteSkillFile(Path.of("SKILL.md"), body)));
        }
        List<RemoteSkillFile> files = new ArrayList<>();
        fetchDirectory(location, location.path(), Path.of(""), 0, budget, files);
        files.sort(Comparator.comparing(file -> file.relativePath().toString()));
        return new RemoteSkillPackage(location.sourceUri(), files);
    }

    private void fetchDirectory(
            RemoteSkillLocation location,
            String remotePath,
            Path relativeRoot,
            int depth,
            SkillDownloadBudget budget,
            List<RemoteSkillFile> output) {
        budget.checkCancelled();
        if (depth > MAX_DEPTH) throw new SkillInstallException("远程 Skill 目录层级超过限制");
        RemoteResponse response = transport.get(contentsUri(location, remotePath), budget);
        JsonNode root;
        try {
            root = json.readTree(response.body());
        } catch (Exception exception) {
            throw new SkillInstallException("GitHub 目录响应格式无效");
        }
        if (!root.isArray()) throw new SkillInstallException("GitHub Skill 地址不是目录");
        List<JsonNode> entries = new ArrayList<>();
        root.forEach(entries::add);
        entries.sort(Comparator.comparing(node -> node.path("name").asText("")));
        for (JsonNode entry : entries) {
            budget.checkCancelled();
            String name = required(entry, "name");
            validateName(name);
            String type = required(entry, "type");
            String itemPath = required(entry, "path");
            ensureWithin(location.path(), itemPath);
            Path relative = relativeRoot.resolve(name).normalize();
            if (relative.isAbsolute() || relative.startsWith("..")) {
                throw new SkillInstallException("GitHub Skill 包含越界路径");
            }
            switch (type) {
                case "dir" -> fetchDirectory(location, itemPath, relative, depth + 1, budget, output);
                case "file" -> {
                    byte[] body = fetchFile(location, itemPath, budget);
                    output.add(new RemoteSkillFile(relative, body));
                }
                case "symlink", "submodule" -> throw new SkillInstallException("远程 Skill 不允许符号链接或子模块");
                default -> throw new SkillInstallException("GitHub Skill 包含不支持的内容类型");
            }
        }
    }

    private byte[] fetchFile(RemoteSkillLocation location, String remotePath, SkillDownloadBudget budget) {
        budget.claimFile();
        RemoteResponse response = transport.get(contentsUri(location, remotePath), budget);
        JsonNode file;
        try {
            file = json.readTree(response.body());
        } catch (Exception exception) {
            throw new SkillInstallException("GitHub 文件响应格式无效");
        }
        if (!file.isObject() || !"file".equals(required(file, "type"))) {
            throw new SkillInstallException("GitHub Skill 文件类型无效");
        }
        String encoding = required(file, "encoding");
        JsonNode content = file.get("content");
        if (!"base64".equalsIgnoreCase(encoding) || content == null || !content.isTextual()) {
            throw new SkillInstallException("GitHub Skill 文件内容不是 base64");
        }
        byte[] body;
        try {
            body = Base64.getMimeDecoder().decode(content.textValue());
        } catch (IllegalArgumentException exception) {
            throw new SkillInstallException("GitHub Skill 文件 base64 无效");
        }
        budget.consumeFileBytes(body.length);
        return body;
    }

    private static URI contentsUri(RemoteSkillLocation location, String path) {
        String encodedPath = encodePath(path);
        String ref = URLEncoder.encode(location.revision(), StandardCharsets.UTF_8);
        return URI.create("https://api.github.com/repos/" + location.owner() + "/"
                + location.repository() + "/contents/" + encodedPath + "?ref=" + ref);
    }

    private static String encodePath(String value) {
        return java.util.Arrays.stream(value.split("/"))
                .map(GitHubSkillFetcher::encodeSegment)
                .reduce((left, right) -> left + "/" + right).orElse("");
    }

    private static String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new SkillInstallException("GitHub 目录项缺少字段: " + field);
        }
        return value.textValue();
    }

    private static void validateName(String name) {
        if (name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\") || name.contains(":")) {
            throw new SkillInstallException("GitHub Skill 包含非法文件名");
        }
    }

    private static void ensureWithin(String root, String candidate) {
        String prefix = root.endsWith("/") ? root : root + "/";
        if (!candidate.startsWith(prefix) || candidate.substring(prefix.length()).isBlank()) {
            throw new SkillInstallException("GitHub Skill 包含越界路径");
        }
    }

    @Override public void cancel() { transport.cancel(); }
}
