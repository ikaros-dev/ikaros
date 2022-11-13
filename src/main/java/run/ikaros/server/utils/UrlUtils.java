package run.ikaros.server.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author li-guohao
 */
public class UrlUtils {

    public static final String PATH_CHAR = "/";

    public static String encode(String originalStr) {
        return URLEncoder.encode(originalStr, StandardCharsets.UTF_8).replace("+", "%20");
    }

}
