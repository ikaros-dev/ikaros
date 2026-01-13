package run.ikaros.server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import run.ikaros.server.infra.consts.IkarosTestConst;

@TestConfiguration(proxyBeanMethods = false)
public class IkarosTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse(IkarosTestConst.DATABASE_IMAGE));
    }

}
