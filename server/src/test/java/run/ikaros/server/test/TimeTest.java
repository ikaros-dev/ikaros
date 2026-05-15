package run.ikaros.server.test;

import java.time.LocalDateTime;
import java.time.Year;
import org.assertj.core.api.Assertions;

public class TimeTest {
    // @Test
    void parseTime() {
        LocalDateTime t1 =
            Year.parse("2020").atMonth(1).atDay(1).atStartOfDay();
        Assertions.assertThat(t1.getYear()).isEqualTo(2020);

        LocalDateTime t2 = LocalDateTime.parse("2020.01");
        Assertions.assertThat(t2.getMonthValue()).isEqualTo(1);
    }
}
