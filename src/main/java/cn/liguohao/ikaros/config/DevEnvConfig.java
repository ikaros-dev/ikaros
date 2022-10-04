package cn.liguohao.ikaros.config;

import cn.liguohao.ikaros.common.kit.FileKit;
import cn.liguohao.ikaros.common.kit.SystemVarKit;
import java.io.File;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author guohao
 * @date 2022/10/04
 */
@Configuration
public class DevEnvConfig {
    private final String env;

    public DevEnvConfig(@Value("${spring.profiles.active}") String env) {
        this.env = env;
    }

    // 开发环境下，启动时清空upload文件夹
    @PostConstruct
    public void cleanUploadDirWhenExist() {
        if ("dev".equalsIgnoreCase(env)) {
            String uploadDirPath =
                SystemVarKit.getCurrentAppDirPath() + File.separatorChar + "upload";
            FileKit.deleteDirByRecursion(uploadDirPath);
        }
    }
}
