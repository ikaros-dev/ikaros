package run.ikaros.server.infra.utils;

public class StringUtils {

    /**
     * upper str first char.
     */
    public static String upperCaseFirst(String val) {
        char[] arr = val.toCharArray();
        arr[0] = Character.toUpperCase(arr[0]);
        return new String(arr);
    }
}
