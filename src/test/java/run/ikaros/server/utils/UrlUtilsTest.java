package run.ikaros.server.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author li-guohao
 */
class UrlUtilsTest {

    @Test
    void encode() {
        String str = "Do It Yourself";
        String encode = UrlUtils.encode(str);
        Assertions.assertNotNull(encode);
    }
}