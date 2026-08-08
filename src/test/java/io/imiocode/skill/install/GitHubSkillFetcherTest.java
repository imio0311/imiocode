package io.imiocode.skill.install;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubSkillFetcherTest {
    @Test
    void downloadsDirectoryPackageWithReferences() {
        MemoryTransport transport = new MemoryTransport();
        transport.add("api.github.com/repos/acme/repo/contents/skills/demo?ref=main", """
                [
                  {"name":"SKILL.md","path":"skills/demo/SKILL.md","type":"file"},
                  {"name":"references","path":"skills/demo/references","type":"dir"}
                ]
                """);
        transport.add("api.github.com/repos/acme/repo/contents/skills/demo/references?ref=main", """
                [{"name":"guide.md","path":"skills/demo/references/guide.md","type":"file"}]
                """);
        transport.add("api.github.com/repos/acme/repo/contents/skills/demo/SKILL.md?ref=main",
                fileResponse(skill("demo")));
        transport.add("api.github.com/repos/acme/repo/contents/skills/demo/references/guide.md?ref=main",
                fileResponse("guide"));

        GitHubSkillFetcher fetcher = new GitHubSkillFetcher(transport);
        RemoteSkillLocation location = new RemoteSkillLocation(
                RemoteSkillKind.GITHUB_TREE, URI.create("https://github.com/acme/repo/tree/main/skills/demo"),
                "acme", "repo", "main", "skills/demo");
        RemoteSkillPackage result = fetcher.fetch(location,
                new SkillDownloadBudget(SkillInstallConfig.defaults()));

        assertEquals(2, result.files().size());
        assertTrue(result.files().stream().anyMatch(file -> file.relativePath().equals(Path.of("SKILL.md"))));
        assertTrue(result.files().stream().anyMatch(file -> file.relativePath().equals(Path.of("references/guide.md"))));
    }

    @Test
    void rejectsLinksAndMissingSkillMarkdown() {
        MemoryTransport links = new MemoryTransport();
        links.add("api.github.com/repos/a/b/contents/skills/demo?ref=main", """
                [{"name":"bad","path":"skills/demo/bad","type":"symlink"}]
                """);
        GitHubSkillFetcher fetcher = new GitHubSkillFetcher(links);
        RemoteSkillLocation location = new RemoteSkillLocation(
                RemoteSkillKind.GITHUB_TREE, URI.create("https://github.com/a/b/tree/main/skills/demo"),
                "a", "b", "main", "skills/demo");
        assertThrows(SkillInstallException.class,
                () -> fetcher.fetch(location, new SkillDownloadBudget(SkillInstallConfig.defaults())));
    }

    private static String skill(String name) {
        return "---\nname: " + name + "\ndescription: demo\n---\n# Demo\nDo work.\n";
    }

    private static String fileResponse(String content) {
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        return "{\"type\":\"file\",\"encoding\":\"base64\",\"content\":\"" + encoded + "\"}";
    }

    private static final class MemoryTransport implements SkillRemoteTransport {
        private final Map<String, byte[]> responses = new HashMap<>();
        void add(String suffix, String value) { responses.put(suffix, value.getBytes(StandardCharsets.UTF_8)); }

        @Override public RemoteResponse get(URI uri, SkillDownloadBudget budget) {
            byte[] body = responses.entrySet().stream()
                    .filter(entry -> uri.toString().contains(entry.getKey()))
                    .map(Map.Entry::getValue).findFirst()
                    .orElseThrow(() -> new AssertionError("unexpected URI: " + uri));
            budget.consumeResponseBytes(body.length);
            return new RemoteResponse(uri, 200, Map.of(), body);
        }
        @Override public void cancel() { }
    }
}
