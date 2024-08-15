package run.ikaros.server.infra.utils;

import java.util.Random;
import lombok.Getter;

public class RandomUtils {
    @Getter
    private static final Random random = new Random();

    /**
     * Random string by length.
     *
     * @param length string length
     * @return string
     */
    public static String randomString(int length) {
        if (length <= 0) {
            length = 10;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

}
