package run.ikaros.server.migration;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MigrationProperties.class)
@ConditionalOnProperty(name = "ikaros.migration.enable", havingValue = "true")
public class MigrationConfiguration {

    /**
     * Create R2dbcEntityTemplate backed by the migration connection factory.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ikaros.migration.datasource", name = "url")
    public MigrationR2dbcEntityTemplate migrationR2dbcEntityTemplate(
            MigrationProperties properties) {
        ConnectionFactory connectionFactory = MigrationConnectionFactoryBuilder.build(properties);
        return new MigrationR2dbcEntityTemplate(connectionFactory);
    }
}
