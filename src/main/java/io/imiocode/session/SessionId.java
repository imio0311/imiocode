package io.imiocode.session;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

public record SessionId(String value) {
    private static final Pattern FORMAT = Pattern.compile("[a-f0-9]{24}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public SessionId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("会话 ID 格式无效");
        }
    }

    public static SessionId generate() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return new SessionId(HexFormat.of().formatHex(bytes));
    }

    @Override public String toString() { return value; }
}
