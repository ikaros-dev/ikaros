package run.ikaros.server.migration;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MigrationProperties.class)
public class MigrationConfiguration {

    /**
     * Create migration connection factory from ikaros.migration.datasource properties.
     */
    @Bean("migrationConnectionFactory")
    @ConditionalOnProperty(prefix = "ikaros.migration.datasource", name = "url")
    public ConnectionFactory migrationConnectionFactory(MigrationProperties properties) {
        return MigrationConnectionFactoryBuilder.build(properties);
    }

    /**
     * Create R2dbcEntityTemplate backed by the migration connection factory.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ikaros.migration.datasource", name = "url")
    public MigrationR2dbcEntityTemplate migrationR2dbcEntityTemplate(
        ConnectionFactory migrationConnectionFactory) {
        return new MigrationR2dbcEntityTemplate(migrationConnectionFactory);
    }
}
