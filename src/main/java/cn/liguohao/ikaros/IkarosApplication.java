package cn.liguohao.ikaros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ikaros Main Class.
 *
 * @author liguohao
 */
@SpringBootApplication
public class IkarosApplication {

    public static void main(String[] args) {
//        // 自定义额外的配置文件位置
//        System.setProperty("spring.config.additional-location",
//            SystemVarKit.getCurrentAppDirPath());

        // 运行应用
        SpringApplication.run(IkarosApplication.class, args);
    }

}
