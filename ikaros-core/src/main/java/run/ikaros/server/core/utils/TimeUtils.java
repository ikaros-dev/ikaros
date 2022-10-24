package run.ikaros.server.core.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author li-guohao
 */
public class TimeUtils {

    public static Date localDataTime2Date(LocalDateTime localDateTime) {
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());

    }

    public static String nowTimestamp() {
        return String.valueOf(new Date().getTime());
    }
}
