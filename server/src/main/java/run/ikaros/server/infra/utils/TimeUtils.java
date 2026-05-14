package run.ikaros.server.infra.utils;


import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
    /**
     * 格式化日期时间戳.
     */
    public static String formatTimestamp(Long timestamp) {
        return formatTimestamp(timestamp, "yyyy-MM-dd");
    }

    /**
     * 格式化日期时间戳.
     */
    public static String formatTimestamp(Long timestamp, String pattern) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
}
