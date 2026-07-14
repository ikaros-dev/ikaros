package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TimeUtilsTest {

    @Test
    void formatTimestamp_defaultPattern_returnsFormatted() {
        String result = TimeUtils.formatTimestamp(1700000000000L);
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void formatTimestamp_customPattern_returnsFormatted() {
        String result = TimeUtils.formatTimestamp(1700000000000L, "yyyy/MM/dd");
        assertThat(result).matches("\\d{4}/\\d{2}/\\d{2}");
    }

    @Test
    void formatTimestamp_epochZero_returnsDate() {
        String result = TimeUtils.formatTimestamp(0L);
        assertThat(result).isEqualTo("1970-01-01");
    }

    @Test
    void formatTimestamp_withTimePattern_includesTime() {
        String result = TimeUtils.formatTimestamp(1700000000000L, "HH:mm:ss");
        assertThat(result).matches("\\d{2}:\\d{2}:\\d{2}");
    }
}
