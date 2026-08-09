package io.imiocode.permission.command;

/** 只解析命令文本，不执行命令或访问网络。 */
@FunctionalInterface
public interface CommandRiskClassifier {
    CommandRiskAssessment classify(String command);
}
