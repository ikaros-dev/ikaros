package run.ikaros.server.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.ikaros.server.config.mybatis.MybatisUUIDTypeHandler;

import java.util.UUID;

@Configuration(proxyBeanMethods = false)
@MapperScan("run.ikaros.server.store.mapper")
public class MybatisConfiguration {
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            configuration.getTypeHandlerRegistry().register(UUID.class, new MybatisUUIDTypeHandler());
        };
    }
}
