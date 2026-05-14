package run.ikaros.api.infra.utils;

import jakarta.annotation.Nullable;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class SystemVarUtils {
    public static final String MEDIA_DIR_NAME = "media";
    public static final String ORIGINAL_DIR_NAME = "original";

    // static {
    //     log.debug("current app dir path: {}", getCurrentAppDirPath());
    //     log.debug("current app user name: {}", getCurrentUserName());
    //     log.debug("current app user home: {}", getCurrentUserDirPath());
    //     log.debug("current ipv4 address: {}", getIpAddress());
    // }

    /**
     * 当前应用的目录路径.
     */
    public static String getCurrentAppDirPath() {
        return System.getProperty("user.dir");
    }


    /**
     * 获取当前登录用户名称.
     */
    public static String getCurrentUserName() {
        return System.getProperty("user.name");
    }

    /**
     * 当前登录用户的home目录路径.
     */
    public static String getCurrentUserDirPath() {
        return System.getProperty("user.home");
    }

    /**
     * 当前的缓存目录.
     */
    public static String getOsCacheDirPath(@Nullable Path basePath) {
        String ikarosCacheFileDir;
        if (basePath == null) {
            String tmpDirPath = System.getProperty("java.io.tmpdir");
            ikarosCacheFileDir = tmpDirPath
                + (tmpDirPath.endsWith(File.separator) ? "" : File.separator) + "ikaros";
        } else {
            String tmpDirPath = basePath.toAbsolutePath().toString();
            ikarosCacheFileDir = tmpDirPath
                + (tmpDirPath.endsWith(File.separator) ? "" : File.separator) + "caches";
        }

        File file = new File(ikarosCacheFileDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        return ikarosCacheFileDir;
    }

    /**
     * Get ip address.
     */
    public static String getIpAddress() {
        InetAddress localHost = null;
        try {
            localHost = Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
        if (localHost == null) {
            return null;
        }
        return localHost.getHostAddress();
    }

    /**
     * Current platform is windows.
     */
    public static boolean platformIsWindows() {
        String osName = System.getProperty("os.name");
        if (!StringUtils.hasText(osName)) {
            return false;
        }
        return osName.contains("Windows");
    }

    public static String getCurrentAppMediaDirPath() {
        return getCurrentAppDirPath() + File.separator + MEDIA_DIR_NAME;
    }

    public static String getCurrentAppOriginalDirPath() {
        return getCurrentAppDirPath() + File.separator + ORIGINAL_DIR_NAME;
    }
}
