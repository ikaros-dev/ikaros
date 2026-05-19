package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TimeUtilsTest {

    @Test
    void formatTimestampDefaultPattern() {
        // 2024-01-15 in UTC
        String result = TimeUtils.formatTimestamp(1705276800000L);
        assertEquals("2024-01-15", result);
    }

    @Test
    void formatTimestampCustomPattern() {
        String result = TimeUtils.formatTimestamp(1705276800000L, "yyyy/MM/dd");
        assertEquals("2024/01/15", result);
    }

    @Test
    void formatTimestampNullThrowsException() {
        assertThrows(NullPointerException.class, () -> TimeUtils.formatTimestamp(null));
    }
}
