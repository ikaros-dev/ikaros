package run.ikaros.server.infra.utils;

import java.nio.charset.StandardCharsets;

public class ByteArrUtils {
    /**
     * 判断字节数组为二进制数据.
     */
    public static boolean isBinaryData(byte[] data) {
        for (byte b : data) {
            if (b < 32 && b != 9 && b != 10 && b != 13) {
                return true; // 存在控制字符，可能是二进制数据
            }
        }
        return false; // 没有控制字符，可能是字符串
    }

    /**
     * 判断字节数组为字符串数据.
     */
    public static boolean isStringData(byte[] data) {
        try {
            String str = new String(data, StandardCharsets.UTF_8);
            // 检查解码后的字符串是否包含非可打印字符
            return str.chars().allMatch(c -> c >= 32 || c == 9 || c == 10 || c == 13);
        } catch (Exception e) {
            return false; // 解码失败，认为是二进制数据
        }
    }

}
