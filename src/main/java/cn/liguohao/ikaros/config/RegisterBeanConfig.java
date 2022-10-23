package cn.liguohao.ikaros.config;

import cn.liguohao.ikaros.model.file.IkarosFileHandler;
import cn.liguohao.ikaros.model.file.LocalIkarosFileHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author guohao
 * @date 2022/10/20
 */
@Configuration
public class RegisterBeanConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


}
