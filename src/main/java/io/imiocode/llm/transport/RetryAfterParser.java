package io.imiocode.llm.transport;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class RetryAfterParser {
    public Optional<Duration> parse(String value, Instant now) {
        if (value == null || value.isBlank() || now == null) {
            return Optional.empty();
        }
        String normalized = value.trim();
        try {
            long seconds = Long.parseLong(normalized);
            return seconds < 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(seconds));
        } catch (NumberFormatException | ArithmeticException ignored) {
            // 继续尝试 HTTP 日期。
        }
        try {
            Instant retryAt = ZonedDateTime.parse(normalized, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            Duration duration = Duration.between(now, retryAt);
            return duration.isNegative() ? Optional.empty() : Optional.of(duration);
        } catch (DateTimeParseException | ArithmeticException exception) {
            return Optional.empty();
        }
    }
}
