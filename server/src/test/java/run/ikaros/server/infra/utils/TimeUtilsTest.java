package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeUtilsTest {

    @Test
    void formatTimestamp() {
        // 2024-01-15 00:00:00 UTC
        Long timestamp = 1705276800000L;
        String result = TimeUtils.formatTimestamp(timestamp);
        assertNotNull(result);
        assertEquals("2024-01-15", result);
    }

    @Test
    void formatTimestampWithPattern() {
        // Use a timestamp that represents midnight in any timezone
        Long timestamp = System.currentTimeMillis();
        String result = TimeUtils.formatTimestamp(timestamp, "yyyy-MM-dd HH:mm:ss");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void formatTimestampWithYearOnly() {
        Long timestamp = 1705276800000L;
        String result = TimeUtils.formatTimestamp(timestamp, "yyyy");
        assertEquals("2024", result);
    }

    @Test
    void formatTimestampWithMonthDay() {
        Long timestamp = 1705276800000L;
        String result = TimeUtils.formatTimestamp(timestamp, "MM-dd");
        assertEquals("01-15", result);
    }
}
