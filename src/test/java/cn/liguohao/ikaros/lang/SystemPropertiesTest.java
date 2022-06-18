package cn.liguohao.ikaros.lang;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class SystemPropertiesTest {
    /**
     * main
     *
     * @param args args
     */
    public static void main(String[] args) {
        String userDir = System.getProperty("user.dir");
        // C:\Develop\GitRepository\ikaros
        System.out.println(userDir);
        String userHome = System.getProperty("user.home");
        // C:\Users\li-guohao
        System.out.println(userHome);
        String userName = System.getProperty("user.name");
        // li-guohao
        System.out.println(userName);
    }
}
