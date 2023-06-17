package run.ikaros.server.test;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateUtilTest {
    @Test
    void test() throws ParseException {
        Date date = DateUtils.parseDate("2023-02-12 11:32:12", "yyyy-MM-dd HH:mm:ss");
        Assertions.assertThat(date).isNotNull();
        ZoneOffset offset = OffsetDateTime.now().getOffset();
        System.out.println(offset);
    }
}
