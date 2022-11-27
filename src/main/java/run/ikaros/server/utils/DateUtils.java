package run.ikaros.server.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author guohao
 * @date 2022/10/20
 */
public class DateUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);

    static final String DEFAULT_PATTERN = "yyyy-MM-dd";

    /**
     * DateKit.parseDateStr("2022-10-01", "yyyy-MM-dd");
     */
    public static Date parseDateStr(String dateStr, String pattern) {
        AssertUtils.notBlank(dateStr, "'dateStr' must not be blank");
        AssertUtils.notBlank(pattern, "'pattern' must not be blank");
        DateFormat fmt = new SimpleDateFormat(pattern);
        try {
            return fmt.parse(dateStr);
        } catch (ParseException e) {
            LOGGER.warn("parse fail, date str: {}, ex msg: {}", dateStr, e.getMessage());
            return null;
        }
    }

    public static Date parseDateStr(String dateStr) {
        return parseDateStr(dateStr, DEFAULT_PATTERN);
    }

}
