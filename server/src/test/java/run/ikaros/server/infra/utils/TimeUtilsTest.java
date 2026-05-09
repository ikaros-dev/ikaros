package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TimeUtilsTest {

    @Test
    void formatTimestampShouldReturnDefaultPattern() {
        // 2024-01-15 00:00:00 UTC
        long timestamp = 1705276800000L;
        String result = TimeUtils.formatTimestamp(timestamp);
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void formatTimestampShouldReturnCustomPattern() {
        long timestamp = 1705276800000L;
        String result = TimeUtils.formatTimestamp(timestamp, "yyyy/MM/dd");
        assertThat(result).contains("/");
    }

    @Test
    void formatTimestampShouldHandleCurrentTime() {
        long now = System.currentTimeMillis();
        String result = TimeUtils.formatTimestamp(now);
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }
}
