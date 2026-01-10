package run.ikaros.server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class IkarosTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLR2DBCDatabaseContainer postgresContainer() {
        return new PostgreSQLR2DBCDatabaseContainer(
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.1-alpine"))
        );
    }

}
