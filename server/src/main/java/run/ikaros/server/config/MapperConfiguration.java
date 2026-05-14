package run.ikaros.server.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("run.ikaros.server.store.mapper")
public class MapperConfiguration {
}
