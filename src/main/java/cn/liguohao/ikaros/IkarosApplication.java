package cn.liguohao.ikaros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * 伊卡洛斯启动
 *
 * @author liguohao
 */
@EnableWebFlux
@SpringBootApplication
public class IkarosApplication {

    public static void main(String[] args) {
        SpringApplication.run(IkarosApplication.class, args);
    }

}
