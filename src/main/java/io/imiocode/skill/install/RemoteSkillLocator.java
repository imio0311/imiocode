package io.imiocode.skill.install;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 将受支持的公开 URL 解析为确定的 GitHub Skill 位置。 */
public final class RemoteSkillLocator {
    private static final Pattern REPO_PART = Pattern.compile("[A-Za-z0-9_.-]{1,100}");
    private final Set<String> allowedHosts;

    public RemoteSkillLocator(SkillInstallConfig config) {
        this.allowedHosts = Objects.requireNonNull(config, "config").allowedHosts();
    }

    public RemoteSkillLocation locate(String value) {
        if (value == null || value.isBlank()) throw new SkillInstallException("Skill URL 不能为空");
        URI uri;
        try {
            uri = new URI(value.trim());
        } catch (URISyntaxException exception) {
            throw new SkillInstallException("Skill URL 格式无效");
        }
        validatePublicUri(uri, allowedHosts);
        if (uri.getRawQuery() != null) throw new SkillInstallException("Skill URL 不允许包含查询参数");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        List<String> segments = pathSegments(uri);
        return switch (host) {
            case "skills.sh" -> parseSkillsSh(uri, segments);
            case "github.com" -> parseGitHubTree(uri, segments);
            case "raw.githubusercontent.com" -> parseRaw(uri, segments);
            default -> throw new SkillInstallException("不支持的 Skill URL 域名: " + host);
        };
    }

    public static void validatePublicUri(URI uri, Set<String> allowedHosts) {
        Objects.requireNonNull(uri, "uri");
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw new SkillInstallException("Skill 下载只允许 HTTPS");
        if (uri.getHost() == null) throw new SkillInstallException("Skill URL 缺少域名");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!allowedHosts.contains(host)) throw new SkillInstallException("不允许访问 Skill 域名: " + host);
        if (uri.getUserInfo() != null) throw new SkillInstallException("Skill URL 不允许包含用户信息");
        if (uri.getPort() != -1) throw new SkillInstallException("Skill URL 不允许自定义端口");
        if (uri.getFragment() != null) throw new SkillInstallException("Skill URL 不允许包含片段");
        String rawPath = Objects.requireNonNullElse(uri.getRawPath(), "");
        String lowered = rawPath.toLowerCase(Locale.ROOT);
        if (lowered.contains("%2f") || lowered.contains("%5c") || rawPath.indexOf('\\') >= 0) {
            throw new SkillInstallException("Skill URL 路径包含非法分隔符");
        }
        for (String segment : rawPath.split("/", -1)) {
            String decoded = URLDecoder.decode(segment, StandardCharsets.UTF_8);
            if (decoded.equals(".") || decoded.equals("..")) throw new SkillInstallException("Skill URL 不允许路径逃逸");
        }
    }

    private static RemoteSkillLocation parseSkillsSh(URI uri, List<String> segments) {
        if (segments.size() != 3) throw new SkillInstallException("skills.sh URL 应为 /owner/repo/skill");
        validateRepoPart(segments.get(0));
        validateRepoPart(segments.get(1));
        validatePathPart(segments.get(2));
        return new RemoteSkillLocation(RemoteSkillKind.SKILLS_SH, uri,
                segments.get(0), segments.get(1), "main", "skills/" + segments.get(2));
    }

    private static RemoteSkillLocation parseGitHubTree(URI uri, List<String> segments) {
        if (segments.size() < 5 || !"tree".equals(segments.get(2))) {
            throw new SkillInstallException("GitHub Skill URL 必须指向 tree 目录");
        }
        validateRepoPart(segments.get(0));
        validateRepoPart(segments.get(1));
        validatePathPart(segments.get(3));
        String path = String.join("/", segments.subList(4, segments.size()));
        validateRelativePath(path);
        return new RemoteSkillLocation(RemoteSkillKind.GITHUB_TREE, uri,
                segments.get(0), stripGitSuffix(segments.get(1)), segments.get(3), path);
    }

    private static RemoteSkillLocation parseRaw(URI uri, List<String> segments) {
        if (segments.size() < 5 || !"SKILL.md".equals(segments.getLast())) {
            throw new SkillInstallException("Raw URL 必须指向 SKILL.md");
        }
        validateRepoPart(segments.get(0));
        validateRepoPart(segments.get(1));
        validatePathPart(segments.get(2));
        String path = String.join("/", segments.subList(3, segments.size()));
        validateRelativePath(path);
        return new RemoteSkillLocation(RemoteSkillKind.GITHUB_RAW, uri,
                segments.get(0), stripGitSuffix(segments.get(1)), segments.get(2), path);
    }

    private static List<String> pathSegments(URI uri) {
        List<String> result = new ArrayList<>();
        for (String value : Objects.requireNonNullElse(uri.getPath(), "").split("/")) {
            if (!value.isBlank()) result.add(value);
        }
        return result;
    }

    private static void validateRepoPart(String value) {
        if (!REPO_PART.matcher(value).matches() || value.equals(".") || value.equals("..")) {
            throw new SkillInstallException("GitHub 仓库路径格式无效");
        }
    }

    private static void validatePathPart(String value) {
        if (value == null || value.isBlank() || value.equals(".") || value.equals("..")
                || value.contains("\\") || value.contains("/")) {
            throw new SkillInstallException("Skill 路径格式无效");
        }
    }

    private static void validateRelativePath(String value) {
        if (value.isBlank()) throw new SkillInstallException("Skill 目录不能为空");
        for (String segment : value.split("/")) validatePathPart(segment);
    }

    private static String stripGitSuffix(String value) {
        return value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }
}
