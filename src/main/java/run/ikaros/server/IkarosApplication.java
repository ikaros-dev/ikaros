package run.ikaros.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import run.ikaros.server.utils.StringUtils;

import java.util.TimeZone;

/**
 * Ikaros Main Class.
 *
 * @author liguohao
 */
@SpringBootApplication
public class IkarosApplication {

    public static void main(String[] args) {
        // 自定义额外的配置文件位置
        // System.setProperty("spring.config.additional-location",
        // SystemVarKit.getCurrentAppDirPath());

        String timeZone = System.getenv("IKAROS_TIME_ZONE");
        if (StringUtils.isBlank(timeZone)) {
            timeZone = "Asia/Shanghai";
        }

        // 时区设置
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));

        // 运行应用
        SpringApplication.run(IkarosApplication.class, args);
    }

}
