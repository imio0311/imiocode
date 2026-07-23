package io.imiocode.terminal;

/** 从可执行 JAR 的 Manifest 解析版本。 */
public final class VersionResolver {
    private VersionResolver() {
    }

    public static String resolve() {
        String version = VersionResolver.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "dev" : version.trim();
    }
}
