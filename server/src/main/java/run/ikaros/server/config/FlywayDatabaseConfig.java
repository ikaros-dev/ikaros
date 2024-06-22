package run.ikaros.server.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.ikaros.api.infra.utils.StringUtils;

@Configuration
@EnableConfigurationProperties({R2dbcProperties.class, FlywayProperties.class})
public class FlywayDatabaseConfig {
    /**
     * Create flyway bean.
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(FlywayProperties flywayProperties, R2dbcProperties r2dbcProperties) {
        final String databaseUrl = StringUtils.isNotBlank(flywayProperties.getUrl())
            ? flywayProperties.getUrl() : convert2flywayDatabaseUrl(r2dbcProperties);
        return Flyway.configure()
            .dataSource(
                databaseUrl,
                r2dbcProperties.getUsername(),
                r2dbcProperties.getPassword()
            )
            .locations(flywayProperties.getLocations().toArray(String[]::new))
            .baselineOnMigrate(true)
            .load();
    }

    private static String convert2flywayDatabaseUrl(R2dbcProperties r2dbcProperties) {
        String jdbcUrl = r2dbcProperties.getUrl().replace("r2dbc:", "jdbc:");
        jdbcUrl = jdbcUrl.substring(0, jdbcUrl.lastIndexOf("?"));
        jdbcUrl = jdbcUrl.replace("///", "");
        return jdbcUrl;
    }
}
