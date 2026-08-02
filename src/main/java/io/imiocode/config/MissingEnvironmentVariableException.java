package io.imiocode.config;

/** 配置引用的启动环境变量不存在。异常只携带变量名，避免泄露配置值。 */
public final class MissingEnvironmentVariableException extends RuntimeException {
    private final String variableName;

    public MissingEnvironmentVariableException(String variableName) {
        super("缺少环境变量: " + requireName(variableName));
        this.variableName = variableName;
    }

    public String variableName() {
        return variableName;
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("variableName 不能为空");
        }
        return value;
    }
}
