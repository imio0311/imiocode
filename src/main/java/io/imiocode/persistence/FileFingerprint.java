package io.imiocode.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** 文件变化的轻量元数据，不读取正文。 */
public record FileFingerprint(Path path, boolean exists, long size, long modifiedMillis, String realPath) {
    public static FileFingerprint capture(Path input) {
        Path path = input.toAbsolutePath().normalize();
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return new FileFingerprint(path, false, 0, 0, "");
        try {
            return new FileFingerprint(path, true, Files.size(path),
                    Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis(), path.toRealPath().toString());
        } catch (IOException exception) {
            return new FileFingerprint(path, true, -1, -1, "unreadable");
        }
    }
}
