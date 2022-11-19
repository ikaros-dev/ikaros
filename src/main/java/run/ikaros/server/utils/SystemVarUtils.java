package run.ikaros.server.utils;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 封装系统变量操作
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class SystemVarUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemVarUtils.class);

    static {
        LOGGER.debug("current app dir path: {}", getCurrentAppDirPath());
        LOGGER.debug("current app user name: {}", getCurrentUserName());
        LOGGER.debug("current app user home: {}", getCurrentUserDirPath());
        LOGGER.debug("current cache tmp dir path: {}", getOsCacheDirPath());
        LOGGER.debug("current ipv4 address: {}", getIPAddress());
    }

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

    public static String getOsCacheDirPath() {
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        String ikarosCacheFileDir = tmpDirPath
            + (tmpDirPath.endsWith(File.separator) ? "" : File.separator) + "ikaros";
        File file = new File(ikarosCacheFileDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        return ikarosCacheFileDir;
    }

    public static String getIPAddress() {
        InetAddress localHost = null;
        try {
            localHost = Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return localHost.getHostAddress();
    }
    
    public static boolean platformIsWindows() {
        return System.getenv("OS").contains("Windows");
    }
}
