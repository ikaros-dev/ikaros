package run.ikaros.server.lang;

public class StringTest {
    public static void main(String[] args) {
        String str = "hello/";
        System.out.println(str.substring(0, str.length() - 1));
        String str2 = "/hello2";
        System.out.println(str2.substring(1));
    }
}
