package io.imiocode.context;

public record CompactReport(long beforeTokens, long afterTokens, int spilledResults,
                            boolean compacted, ContextOutcome outcome, String message) {
    public CompactReport {
        if (beforeTokens < 0 || afterTokens < 0 || spilledResults < 0) {
            throw new IllegalArgumentException("压缩统计不能为负数");
        }
        message = message == null ? "" : message;
    }

    public double savedRatio() {
        return beforeTokens == 0 ? 0d : Math.max(0d, (double) (beforeTokens - afterTokens) / beforeTokens);
    }
}
