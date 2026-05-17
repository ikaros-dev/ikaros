package run.ikaros.server.migration;

import java.time.*;
import java.sql.Timestamp;

public class DateTimeConverter {

    // 存储到数据库：将LocalDateTime按指定时区转为Timestamp
    public static Timestamp toTimestamp(LocalDateTime localDateTime, ZoneId zoneId) {
        if (localDateTime == null) return null;
        // LocalDateTime + 时区 = ZonedDateTime -> 转为Instant -> Timestamp
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        return Timestamp.from(zonedDateTime.toInstant());
    }

    // 从数据库读取：将Timestamp转为指定时区的LocalDateTime
    public static LocalDateTime toLocalDateTime(Timestamp timestamp, ZoneId zoneId) {
        if (timestamp == null) return null;
        Instant instant = timestamp.toInstant();
        return instant.atZone(zoneId).toLocalDateTime();
    }
}
