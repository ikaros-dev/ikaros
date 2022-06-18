package cn.liguohao.ikaros.common;

/**
 * 封装系统变量操作
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class SystemVarKit {

    /**
     * @return 当前应用的目录路径
     */
    public static String getCurrentAppDirPath() {
        return System.getProperty("user.dir");
    }


    /**
     * @return 获取当前登录用户名称
     */
    public static String getCurrentUserName() {
        return System.getProperty("user.name");
    }

    /**
     * @return 当前登录用户的home目录路径
     */
    public static String getCurrentUserDirPath() {
        return System.getProperty("user.home");
    }

}
