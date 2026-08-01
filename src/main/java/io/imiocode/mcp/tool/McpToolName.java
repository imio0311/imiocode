package io.imiocode.mcp.tool;

/** 将 Server 名和远程工具名转换为稳定的本地工具名。 */
public final class McpToolName {
    private static final int MAX_LENGTH = 64;

    private McpToolName() {
    }

    public static String create(String serverName, String remoteToolName) {
        String server = sanitize(serverName, "serverName");
        String tool = sanitize(remoteToolName, "remoteToolName");
        String result = "mcp_" + server + "__" + tool;
        if (result.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("MCP 工具名称超过 64 字符");
        }
        return result;
    }

    private static String sanitize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if ((codePoint >= 'a' && codePoint <= 'z')
                    || (codePoint >= 'A' && codePoint <= 'Z')
                    || (codePoint >= '0' && codePoint <= '9')
                    || codePoint == '_' || codePoint == '-') {
                result.appendCodePoint(codePoint);
            } else {
                result.append('_');
            }
            offset += Character.charCount(codePoint);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " 清洗后为空");
        }
        return result.toString();
    }
}
