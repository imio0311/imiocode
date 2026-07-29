package io.imiocode.prompt;

/** 每个用户任务调用一次的环境采集抽象。 */
@FunctionalInterface
public interface EnvironmentContextProvider {
    EnvironmentContext capture();
}
