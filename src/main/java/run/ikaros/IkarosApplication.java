package run.ikaros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ikaros 应用启动入口，承载全部模块化单体能力。
 */
@SpringBootApplication
public class IkarosApplication {

    /**
     * 启动 Ikaros 服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(IkarosApplication.class, args);
    }
}
