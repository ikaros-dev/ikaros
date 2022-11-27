package run.ikaros.server.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author guohao
 * @date 2022/09/07
 */
public class TimeUtils {

    public static Date localDataTime2Date(LocalDateTime localDateTime) {
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    public static LocalDateTime date2LocalDataTime(Date date) {
        AssertUtils.notNull(date, "date");
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static String nowTimestamp() {
        return String.valueOf(new Date().getTime());
    }
}
