package cn.liguohao.ikaros.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author guohao
 * @date 2022/09/07
 */
public class TimeKit {

    public static Date localDataTime2Date(LocalDateTime localDateTime) {
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());

    }

    public static String nowTimestamp() {
        return String.valueOf(new Date().getTime());
    }
}
